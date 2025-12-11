import os
import sys
import argparse
import pandas as pd
import numpy as np
from datetime import datetime

from cleanlab.dataset import health_summary
from cleanlab.classification import CleanLearning

from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import cross_val_predict
from sklearn.linear_model import LogisticRegression
from sentence_transformers import SentenceTransformer

sys.path.append(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
from config import LABEL_CLEANING_CONFIG, MODELS_DIR


SEED_GLOBAL = 42


def dataset_health_analysis(label_header, texts_encoded=None, df=None):
    model = LogisticRegression(max_iter=500, random_state=SEED_GLOBAL)
    
    label_standard = df[label_header].factorize(sort=True)[0]
    pred_probs = cross_val_predict(model, texts_encoded, label_standard, cv=5, method="predict_proba")

    summary = health_summary(labels=label_standard, pred_probs=pred_probs, verbose=False)

    # add label text to columns
    label_text_map_alphabetical = {i: label_text for i, label_text in enumerate(df[label_header].factorize(sort=True)[1])}
    summary["classes_by_label_quality"]["Class Text"] = summary["classes_by_label_quality"]["Class Index"].map(label_text_map_alphabetical)
    summary["overlapping_classes"]["Class Label A"] = summary["overlapping_classes"]["Class Index A"].map(label_text_map_alphabetical)
    summary["overlapping_classes"]["Class Label B"] = summary["overlapping_classes"]["Class Index B"].map(label_text_map_alphabetical)

    return summary


def save_health_summary_to_file(summary_dict, file):
    file.write(f"Overall label health score:\n{summary_dict['overall_label_health_score']}")
    file.write("\n\n")  
    file.write("Classes by label quality:\n")
    file.write(summary_dict["classes_by_label_quality"].to_string(index=False))  
    file.write("\n\n") 
    file.write("Overlapping classes:\n")
    file.write(summary_dict["overlapping_classes"].to_string(index=False))  


def find_label_issues(threshold, texts_encoded=None, data=None):
    """
    Find label issues using CleanLearning with the given classifier and data.

    Parameters:
    - data (Dataframe): Data to clean.

    Returns:
    - Cleaned data (Dataframe), Dirty data (Dataframe)
    """
    cleaning_stats = {}
    model = LogisticRegression(max_iter=500, random_state=SEED_GLOBAL)
    cl = CleanLearning(model, seed=SEED_GLOBAL, **LABEL_CLEANING_CONFIG["cleanlab"]["clean_learning_params"])
    
    raw_labels = data[LABEL_CLEANING_CONFIG["data_label_header"]].to_list()
    encoder = LabelEncoder()
    encoder.fit(raw_labels)
    labels = encoder.transform(raw_labels)

    label_issues = cl.find_label_issues(X=texts_encoded, labels=labels, **LABEL_CLEANING_CONFIG["cleanlab"]["find_label_issues_params"]) 
    
    data_label_issues = data.join(label_issues)
    data_label_issues.drop(["given_label", "predicted_label"], axis=1, inplace=True)
    
    data_to_remove_1 = data_label_issues[data_label_issues["is_label_issue"] == True]
    print("\nIssues identified via prune_by_noise_rate: ", len(data_to_remove_1))
    cleaning_stats["Issues identified via prune_by_noise_rate"] = len(data_to_remove_1)

    # Step 1: Sort the dataframe by quality and identify the top N for each class to keep at least N
    sorted_df = data_label_issues.sort_values(by=[LABEL_CLEANING_CONFIG["data_label_header"], 'label_quality'], ascending=[True, False])
    df_top_n_per_class = sorted_df.groupby(LABEL_CLEANING_CONFIG["data_label_header"]).head(LABEL_CLEANING_CONFIG["min_label_class_size"])
    # Step 2: Filter out rows below the threshold
    df_low_quality = data_label_issues[data_label_issues.label_quality < threshold] 
    # Step 3: keep at least the top N for each class among low quality
    data_to_remove_2 = df_low_quality[~df_low_quality.index.isin(df_top_n_per_class.index)]
    print("Issues identified via label quality: ", len(data_to_remove_2))
    cleaning_stats["Issues identified via label quality"] = len(data_to_remove_2)

    indices_to_remove = set(data_to_remove_1.index.to_list() + data_to_remove_2.index.to_list())
    print("Total texts to remove: ", len(indices_to_remove))
    cleaning_stats["Total texts to remove"] = len(indices_to_remove)
    
    data_without_label_issues = data_label_issues[~data_label_issues.index.isin(indices_to_remove)]
    data_with_label_issues = data_label_issues[data_label_issues.index.isin(indices_to_remove)]
    data_without_label_issues.drop("is_label_issue", axis=1, inplace=True)
    data_with_label_issues.drop("is_label_issue", axis=1, inplace=True)
    
    return data_without_label_issues, data_with_label_issues, cleaning_stats 


def find_label_issues_and_relabel(threshold, texts_encoded=None, data=None):
    """
    Find label issues using CleanLearning with the given classifier and data.
    Also, train classifier on clean data and predict on data with noisy labels.
    
    Returns:
    - Cleaned data (Dataframe)
    """
    cleaning_stats = {}
    model = LogisticRegression(max_iter=500, random_state=SEED_GLOBAL)
    cl = CleanLearning(model, seed=SEED_GLOBAL, **LABEL_CLEANING_CONFIG["cleanlab"]["clean_learning_params"])
    
    raw_labels = data[LABEL_CLEANING_CONFIG["data_label_header"]].to_list()
    encoder = LabelEncoder()
    encoder.fit(raw_labels)
    labels = encoder.transform(raw_labels)

    label_issues = cl.find_label_issues(X=texts_encoded, labels=labels, **LABEL_CLEANING_CONFIG["cleanlab"]["find_label_issues_params"]) 
    
    data_label_issues = data.join(label_issues)
    data_label_issues.drop(["given_label", "predicted_label"], axis=1, inplace=True)
    
    data_to_remove_1 = data_label_issues[data_label_issues["is_label_issue"] == True]
    print("\nIssues identified via prune_by_noise_rate: ", len(data_to_remove_1))
    cleaning_stats["Issues identified via prune_by_noise_rate"] = len(data_to_remove_1)
    
    # Step 1: Sort the dataframe by quality and identify the top N for each class to keep at least N
    sorted_df = data_label_issues.sort_values(by=[LABEL_CLEANING_CONFIG["data_label_header"], 'label_quality'], ascending=[True, False])
    df_top_n_per_class = sorted_df.groupby(LABEL_CLEANING_CONFIG["data_label_header"]).head(LABEL_CLEANING_CONFIG["min_label_class_size"])
    # Step 2: Filter out rows below the threshold
    df_low_quality = data_label_issues[data_label_issues.label_quality < threshold] 
    # Step 3: keep at least the top N for each class among low quality
    data_to_remove_2 = df_low_quality[~df_low_quality.index.isin(df_top_n_per_class.index)]
    print("Issues identified via label quality: ", len(data_to_remove_2))
    cleaning_stats["Issues identified via label quality"] = len(data_to_remove_2)
 
    indices_to_predict = list(set(data_to_remove_1.index.to_list() + data_to_remove_2.index.to_list()))
    indices_to_predict.sort()
    print("Total labels to clean: ", len(indices_to_predict))
    cleaning_stats["Total re-labelled texts"] = len(indices_to_predict)

    cl.fit(X=texts_encoded, labels=labels, label_issues=cl.get_label_issues())
    texts_encoded_to_predict = np.array([texts_encoded.tolist()[i] for i in indices_to_predict])
    pred_labels = cl.predict(texts_encoded_to_predict)
    
    data_label_issues["cleanlab_predicted_labels"] = raw_labels
    data_label_issues.loc[indices_to_predict, "cleanlab_predicted_labels"] = encoder.inverse_transform(pred_labels)
    
    return data_label_issues, cleaning_stats 

    
if __name__ == "__main__":
    startTime = datetime.now()
    parser = argparse.ArgumentParser(
        prog="run_cleanlab_errorcheck",
        description="""Run cleanlab data cleaning on a file""")
    parser.add_argument("-f","--file-path", type=str, dest='filepath', required=True, help="tsv file to clean with full path")
    parser.add_argument("-s","--score-threshold", type=float, dest='score_threshold',choices=[round(i * 0.1, 1) for i in range(11)], help="Label quuality threshold between [0.0 - 1]",default=0.3)
    parser.add_argument('--relabel', action="store_true", help='add this flag if you want to relabel error labels found in dataset')
    
    args = parser.parse_args()    
        
    data = pd.read_table(args.filepath)
    
    data = data[data[LABEL_CLEANING_CONFIG["data_label_header"]].isin(LABEL_CLEANING_CONFIG["labels"])]
    data.reset_index(inplace=True, drop=True)
    
    sbert_embedder = SentenceTransformer(os.path.join(MODELS_DIR, LABEL_CLEANING_CONFIG['embedder_model']['name']))
    
    # Get data health summary before cleaning
    raw_texts = data[LABEL_CLEANING_CONFIG["data_text_header"]].to_list()
    texts_encoded = sbert_embedder.encode(raw_texts, **LABEL_CLEANING_CONFIG['embedder_model']['params'])
    data_health = dataset_health_analysis(LABEL_CLEANING_CONFIG["data_label_header"], texts_encoded, data)
    
    if args.relabel:
        data_clean, cleaning_stats = find_label_issues_and_relabel(args.score_threshold, texts_encoded, data)
        
        file_root, filename = os.path.split(args.filepath)[0], os.path.split(args.filepath)[1] 
        clean_file_path = os.path.join(file_root, f"{filename.split('.')[0]}_cleaned.tsv")
        data_clean.to_csv(clean_file_path, index=False, sep="\t", lineterminator="\n")      

        # Get data health summary after cleaning 
        clean_texts = data_clean[LABEL_CLEANING_CONFIG["data_text_header"]].to_list()
        clean_texts_encoded = sbert_embedder.encode(clean_texts, **LABEL_CLEANING_CONFIG['embedder_model']['params']) 
        cleaned_data_health = dataset_health_analysis("cleanlab_predicted_labels", clean_texts_encoded, data_clean)
    else:
        data_clean, data_dirty, cleaning_stats = find_label_issues(args.score_threshold, texts_encoded, data)
        
        file_root, filename = os.path.split(args.filepath)[0], os.path.split(args.filepath)[1] 
        clean_file_path = os.path.join(file_root, f"{filename.split('.')[0]}_clean.tsv")
        dirty_file_path = os.path.join(file_root, f"{filename.split('.')[0]}_dirty.tsv")
        data_clean.to_csv(clean_file_path, index=False, sep="\t", lineterminator="\n")
        data_dirty.to_csv(dirty_file_path, index=False, sep="\t", lineterminator="\n")
    
        # Get data health summary after cleaning 
        clean_texts = data_clean[LABEL_CLEANING_CONFIG["data_text_header"]].to_list()
        clean_texts_encoded = sbert_embedder.encode(clean_texts, **LABEL_CLEANING_CONFIG['embedder_model']['params']) 
        cleaned_data_health = dataset_health_analysis(LABEL_CLEANING_CONFIG["data_label_header"], clean_texts_encoded, data_clean)
    
    with open(os.path.join(file_root, "health_summary.txt"), "w") as file: 
        file.write("CLEANING STATS:\n\n")
        for key, value in cleaning_stats.items():
            file.write(f"{key}:, {value}\n")
        file.write("\n\n\n")    
        file.write("BEFORE CLEANING:\n\n")
        save_health_summary_to_file(data_health, file)
        file.write("\n\n\n") 
        file.write("AFTER CLEANING:\n\n")
        save_health_summary_to_file(cleaned_data_health, file)
               
    execution_time = datetime.now() - startTime
    days = execution_time.days
    hours, remainder = divmod(execution_time.seconds, 3600)
    minutes, seconds = divmod(remainder, 60)
    
    print(f"\nExecution time: {days}D:{hours}H:{minutes}M:{seconds}S.{execution_time.microseconds}")    
                
                