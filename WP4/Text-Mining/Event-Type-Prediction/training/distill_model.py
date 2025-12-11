import pandas as pd

from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    AutoConfig,
    pipeline
)
import torch
import torch.nn.functional as F
import argparse
from datetime import datetime
import os
from json import dump

from utils import label2id, id2label, get_num_samples
from train import TweetDataset
from test import test_model


def count_parameters(model):
    """
    Returns the total and trainable parameter counts of a Hugging Face transformer model.
    """
    total_params = sum(p.numel() for p in model.parameters())
    trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    return {
        "total": total_params,
        "trainable": trainable_params,
        "non_trainable": total_params - trainable_params
    }


def main(pretrained_model_path, train_file_path, test_file_path, max_length, experiment_id, hidden_layers,
         experiment_suffix, results_folder):
    if experiment_suffix:
        suffix = "_" + experiment_suffix
    else:
        suffix = experiment_suffix

    if not experiment_id:
        now = datetime.now()
        experiment_id = now.strftime("%d%m%Y_%H%M%S") + f"_h{hidden_layers}" +  suffix
    else:
        experiment_id = args.experiment_id + suffix
    experiment_folder = os.path.join(results_folder, experiment_id)

    os.makedirs(results_folder, exist_ok=True)
    os.makedirs(experiment_folder, exist_ok=True)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    student_checkpoint = "cardiffnlp/twitter-xlm-roberta-base" # Base XLM-T before finetuning
    num_labels = len(label2id)
    temperature = 2.0
    alpha = 0.7

    tokenizer = AutoTokenizer.from_pretrained(pretrained_model_path)
    teacher_model = AutoModelForSequenceClassification.from_pretrained(pretrained_model_path).to(device)

    print("Teacher model: ")
    print(count_parameters(teacher_model))

    # Load base config and modify for smaller student
    student_config = AutoConfig.from_pretrained(student_checkpoint)
    student_config.num_hidden_layers = hidden_layers  # reduce layers
    student_config.num_labels = num_labels

    # Initialize student from config (random weights)
    student_model = AutoModelForSequenceClassification.from_config(student_config).to(device)

    print("Student model: ")
    print(count_parameters(student_model))

    def copy_encoder_layers(teacher, student):
        teacher_layers = teacher.roberta.encoder.layer
        student_layers = student.roberta.encoder.layer

        assert len(student_layers) <= len(teacher_layers), "Student can't have more layers than teacher!"

        step = len(teacher_layers) // len(student_layers)

        for i, student_layer in enumerate(student_layers):
            teacher_layer_idx = i * step
            student_layer.load_state_dict(teacher_layers[teacher_layer_idx].state_dict())
            print(f"Copied teacher layer {teacher_layer_idx} to student layer {i}")

    # Run copying
    copy_encoder_layers(teacher_model, student_model)

    # Copy embeddings
    student_model.roberta.embeddings.load_state_dict(teacher_model.roberta.embeddings.state_dict())
    print("Copied embeddings")

    # Copy classifier
    student_model.classifier.load_state_dict(teacher_model.classifier.state_dict())
    print("Copied classifier head")

    # Freeze teacher
    for param in teacher_model.parameters():
        param.requires_grad = False
    teacher_model.eval()

    train_df = pd.read_csv(train_file_path, sep="\t")
    train_dataset = TweetDataset(train_df, tokenizer, max_length, label2id)

    student_model.config.label2id = label2id
    student_model.config.id2label = id2label

    # === Custom Trainer ===
    class DistillationTrainer(Trainer):
        def __init__(self, teacher_model, temperature=2.0, alpha=0.5, **kwargs):
            super().__init__(**kwargs)
            self.teacher = teacher_model
            self.temperature = temperature
            self.alpha = alpha

        def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
            labels = inputs['labels']

            student_outputs = model(
                input_ids=inputs["input_ids"],
                attention_mask=inputs["attention_mask"]
            )
            student_logits = student_outputs.logits

            with torch.no_grad():
                teacher_outputs = self.teacher(
                    input_ids=inputs["input_ids"],
                    attention_mask=inputs["attention_mask"]
                )
                teacher_logits = teacher_outputs.logits

            # Distillation loss (KL)
            T = self.temperature
            loss_kl = F.kl_div(
                F.log_softmax(student_logits / T, dim=-1),
                F.softmax(teacher_logits / T, dim=-1),
                reduction="batchmean"
            ) * (T * T)

            # Classification loss (ground truth)
            loss_ce = F.cross_entropy(student_logits, labels)

            # Combine
            loss = self.alpha * loss_kl + (1. - self.alpha) * loss_ce
            return (loss, student_outputs) if return_outputs else loss

    # === Training setup ===
    training_args = TrainingArguments(
        output_dir=experiment_folder,
        per_device_train_batch_size=16,
        per_device_eval_batch_size=16,
        num_train_epochs=3,
        learning_rate=3e-5,
        # evaluation_strategy="epoch",
        logging_strategy="epoch",
        save_strategy="epoch",
        save_total_limit=2,
        fp16=torch.cuda.is_available(),
        disable_tqdm=False
    )

    trainer = DistillationTrainer(
        model=student_model,
        teacher_model=teacher_model,
        args=training_args,
        train_dataset=train_dataset,
        tokenizer=tokenizer,
        temperature=temperature,
        alpha=alpha,
    )

    trainer.train()

    test_df = pd.read_csv(test_file_path, sep="\t")

    pipe = pipeline("text-classification", model=trainer.model, tokenizer=tokenizer, max_length=max_length,
                    truncation=True, device=device)

    test_report = test_model(pipe, test_df, os.path.join(experiment_folder, "test_report.tsv"), experiment_id)

    # get train samples for the report
    languages = train_df["tweet_language"].unique().tolist()
    data = []
    for lang in languages:
        lang_train_df = train_df.loc[train_df['tweet_language'] == lang]
        num_train_samples = get_num_samples(lang_train_df)
        row = [lang, num_train_samples["fire"], num_train_samples["flood"], num_train_samples["none"]]
        data.append(row)

    print(data)

    df_train_samples = pd.DataFrame(data, columns=["language", "train_num_fire", "train_num_flood", "train_num_none"])

    total_report = pd.merge(df_train_samples, test_report, on="language")
    total_report.to_csv(os.path.join(experiment_folder, "total_report.tsv"), index=False, sep="\t", float_format='%.3f')

    # model.precision_recall_curve(test_dataset)
    # print("Finished prc")
    tokenizer.save_pretrained(os.path.join(experiment_folder, "tokenizer"))
    dump(id2label, open(os.path.join(experiment_folder, "id2label.json"), "w"), indent=4)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        prog='main',
        description='Distill a fine-tuned BERT to predict event type regarding weather disasters') 
    parser.add_argument("-p", '--pretrained_model_path', type=str, required=True)
    parser.add_argument('-tr', '--train_file_path', type=str, required=True)
    parser.add_argument('-te', '--test_file_path', type=str, required=True)
    parser.add_argument("-ml", '--max_length', type=int, default=50)
    parser.add_argument("-hl", '--hidden_layers', type=int, required=True)
    parser.add_argument("-ei", '--experiment_id', type=str, required=True)
    parser.add_argument("-es", '--experiment_suffix', type=str, default="")
    parser.add_argument("-r", '--results_folder', type=str, default="results")
    args = parser.parse_args()

    main(args.pretrained_model_path, args.train_file_path, args.test_file_path, args.max_length, args.experiment_id,
         args.hidden_layers, args.experiment_suffix, args.results_folder)
