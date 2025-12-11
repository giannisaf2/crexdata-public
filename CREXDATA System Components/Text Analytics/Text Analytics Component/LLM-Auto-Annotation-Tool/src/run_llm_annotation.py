import os
import sys
import argparse
import pandas as pd
from tqdm import tqdm

from datetime import datetime
from annotate_utils import annotate

sys.path.append(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
from config import ANNOTATION_CONFIG, MODELS_CONFIG

def annotate_dataset(data, text_header, model_name, with_confidence, fewshot, fewshot_type):
    """
    Inputs: 
    data: DataFrame with texts to annotate
    text_header: STRING which is the text column name in dataframe
    model_name: STRING which is the name of predefined models in annotate_utils.py
    with_confidence: BOOL, True for annotation with confidence and False for without 
    
    Output:
    unannotated: dataframe with annotations in predicated_labels column, and confidence in confidence_score column
    
    Note: 
    confidence_score column contains NONE if with_confidence is False.
    If an annotation is not among provided labels, annotation is set to NONE
    """
    if fewshot:
        fewshot_df = pd.read_table(fewshot)
    else:
        fewshot_df = pd.DataFrame()
        
    unannotated = data.copy()
    
    print("[INFO]: Beginning annotation..")
    print(f"\nAnnotation Details:\nModel - {model_name}\nDataset size - {len(unannotated.index)}\nWith Confidence - {with_confidence}")
    
    tqdm.pandas(desc="Annotating....")
    labels_and_scores = unannotated[text_header].progress_apply(annotate, args=(model_name, fewshot_df, fewshot_type, with_confidence))
    unannotated['predicted_labels'] = [x[0] for x in labels_and_scores]
    unannotated['confidence_score'] = [x[1] for x in labels_and_scores]
    unannotated['predicted_labels'] = unannotated['predicted_labels'].apply(lambda x: x if x in ANNOTATION_CONFIG['labels'] else 'none')
        
    return unannotated

def main():
    parser = argparse.ArgumentParser(
        description='Annotate a tsv file with disaster social media posts')
    parser.add_argument('-f', '--file-path', dest='file_path', type=str, required=True, help='Path of tsv file to annotate')
    parser.add_argument('--text-header', dest='text_header', type=str, required=True, help='tweet or text column name in tsv file')
    
    parser.add_argument('--modelname', default='phi3mini', const='phi3mini', nargs='?',
                        choices=list(MODELS_CONFIG.keys()), 
                        help='model name supports phi3mini, oo-phi1_5..  (default: %(default)s)') 
    
    parser.add_argument('-o', '--output-path', dest='out_path', type=str, required=True, help='Path to save tsv')
    parser.add_argument('-e', '--fewshot', dest='fewshot', type=str, nargs='?', const=False, default=False, help='Few shot file path for few shot prompting')
    parser.add_argument('--fewshot-type', dest='fewshot_type', type=str, default="in_chat", help='Type of fewshot options: in_chat, in_prompt default: in_chat')
    parser.add_argument('--with-confidence', dest='with_confidence', action='store_true', help='Add this flag to annotate with confidence')
    args = parser.parse_args()
    
    os.makedirs(args.out_path, exist_ok=True)
    textcol = args.text_header  
    data = pd.read_table(args.file_path)
    data = data.dropna(subset=[textcol])
    
    annotated_df = annotate_dataset(data, textcol, args.modelname, args.with_confidence, args.fewshot, args.fewshot_type)
    
    filename = os.path.split(args.file_path)[1]
    if args.with_confidence:
        save_filename = filename.split(".")[0]+f"-{args.modelname}-with_confidence"
    else:
        save_filename = filename.split(".")[0]+f"-{args.modelname}-without_confidence"
        annotated_df.drop(columns=['confidence_score'], inplace=True)
        
    if args.fewshot:
        save_filename += "-fewshot"
        
    annotated_file_path = os.path.join(args.out_path, save_filename+"-annotated.tsv") 
    annotated_df.to_csv(annotated_file_path, index=False, sep="\t", lineterminator="\n")

if __name__ == "__main__":
    startTime = datetime.now()
    main()
    execution_time = datetime.now() - startTime
    days = execution_time.days
    hours, remainder = divmod(execution_time.seconds, 3600)
    minutes, seconds = divmod(remainder, 60)
    
    print(f"\nExecution time: {days}D:{hours}H:{minutes}M:{seconds}S.{execution_time.microseconds}")