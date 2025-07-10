import argparse
from transformers import AutoModelForSequenceClassification
import torch
from fdaopt.training import get_evaluate_fn


def main():
    parser = argparse.ArgumentParser(description="Evaluate a fine-tuned Hugging Face model on a dataset.")
    
    parser.add_argument('--model_checkpoint', type=str, required=True,
                        help="Hugging Face model checkpoint name (e.g., 'prajjwal1/bert-tiny')")
    
    parser.add_argument('--local_weights', type=str, required=True,
                        help="Path to local .pth file with saved model weights.")
    
    parser.add_argument('--ds_path', type=str, default="glue",
                        help="Dataset path (e.g., 'glue')")
    
    parser.add_argument('--ds_name', type=str, default="mrpc",
                        help="Dataset name (e.g., 'mrpc')")
    
    parser.add_argument('--device', type=str, default="cpu",
                        help="Device to use (e.g., 'cuda:0' or 'cpu')")
    
    parser.add_argument('--num_labels', type=int, default=2,
                        help="Number of labels for classification task.")
    
    args = parser.parse_args()
    
    # Step 1: Load model architecture
    model = AutoModelForSequenceClassification.from_pretrained(
        args.model_checkpoint, num_labels=args.num_labels
    )
    
    # Step 2: Load weights
    state_dict = torch.load(args.local_weights, map_location=args.device)
    model.load_state_dict(state_dict)

    # Step 3: Evaluate
    evaluate_fn = get_evaluate_fn(
        model=model,
        device=args.device,
        model_checkpoint=args.model_checkpoint,
        ds_path=args.ds_path,
        ds_name=args.ds_name
    )

    metrics = evaluate_fn(-1, model.parameters(), None)
    metrics = metrics[-1]
    
    print()
    print("-----Evaluation metrics-----")
    for name, val in metrics.items():
        print(f"{name}: {val}")
    print("----------------------------")
    print()


if __name__ == "__main__":
    main()
