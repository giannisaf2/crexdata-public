import os
import sys
import numpy as np
import requests
from tqdm import tqdm
import gradio as gr

# Import the PyFLAGR modules for rank aggregation
import pyflagr.Linear as Linear
import pyflagr.Majoritarian as Majoritarian

from operator import itemgetter

from haystack import Document
# from haystack.pipeline import Pipeline
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack.components.embedders import SentenceTransformersDocumentEmbedder
from haystack.components.retrievers.in_memory import InMemoryEmbeddingRetriever

from sentence_transformers import SentenceTransformer

sys.path.append(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
from config import QA_CONFIG, ROOT_DIR




class LLMGenerator:
    def __init__(self, llm_model):
        self.llm_model = llm_model

    # @spaces.GPU(duration=120)
    def generate_answer(self, texts, query, response_lang, mode='validate'):
        
        template_texts =""
        for i, text in enumerate(texts):
            template_texts += f'{i+1}. {text} \n'

        if mode == 'validate':
            conversation = [ {'role': 'user', 'content': f'Given the following query: "{query}"? \nIs the following document relevant to answer this query?\n{template_texts} \nRespond with exactly: Yes / No'} ]
        elif mode == 'summarize':
            conversation = [ {'role': 'user', 'content': f'For the following query and documents, try to answer the given query based on the documents. Reply ONLY in {response_lang} regardless of the language in the documents.\nQuery: {query} \nDocuments: {template_texts}.'} ]
        elif mode == 'h_summarize':
            conversation = [ {'role': 'user', 'content': f'The documents below describe a developing disaster event. Based on these documents, write a brief summary in the form of a paragraph, highlighting the most crucial information. Reply ONLY in {response_lang} regardless of the language in the documents.\nDocuments: {template_texts}'} ]
        
        if self.llm_model is not None:    
            self.llm_model.reset()
            response = self.llm_model.create_chat_completion(conversation, temperature=0.7, top_p=0.95, top_k=10, min_p=0.05, repeat_penalty=1.1, max_tokens=4096) 
            assistant_respond = response["choices"][0]["message"]["content"]
        else:
            url = (f"{QA_CONFIG['aihub_params']['endpoint_url']}"
                   f"min_p={QA_CONFIG['aihub_params']['min_p']}&"
                   f"model={QA_CONFIG['aihub_params']['model']}&"
                   f"top_k={QA_CONFIG['aihub_params']['top_k']}&"
                   f"top_p={QA_CONFIG['aihub_params']['top_p']}&"
                   f"max_tokens={QA_CONFIG['aihub_params']['max_tokens']}&"
                   f"temperature={QA_CONFIG['aihub_params']['temperature']}&"
                   f"repeat_penalty={QA_CONFIG['aihub_params']['repeat_penalty']}")
            
            headers = {
                "Content-Type": "application/json",
                "Authorization": f"apitoken {QA_CONFIG['aihub_apitoken']}" 
                }
            data = {"data":[{"prompt": conversation[0]["content"]}]}
            response = requests.post(url, headers=headers, json=data)
            
            if f"{response.status_code}" != "200":
                gr.Ercror("Something wrong with AIHUB endpoint", duration=None)
                
            qa_response = response.json()
            assistant_respond = qa_response["data"][0]["qa_response"]
            
        if mode == 'validate':
            if 'Yes' in assistant_respond:
                return True
            else:
                return False
        elif mode == 'summarize':
            return assistant_respond
        elif mode == 'h_summarize':
            return assistant_respond


class QAIndexer:
    def __init__(self, index_type, emb_model):
        self.document_embedder = SentenceTransformersDocumentEmbedder(model=emb_model)
        self.document_embedder.warm_up()
        if index_type == 'in_memory':
            self.document_store = InMemoryDocumentStore(embedding_similarity_function="cosine")


    def index(self, docs_to_index):
        documents_with_embeddings = self.document_embedder.run(docs_to_index)
        self.document_store.write_documents(documents_with_embeddings['documents'])

    
    def index_dataframe(self, data):
        docs_to_index = self.read_dataframe(data)
        self.index(docs_to_index)
    
    def index_stream(self, stream_data):
        docs_to_index = self.read_stream(stream_data)
        self.index(docs_to_index)
    
    def read_dataframe(self, data):
        # Convert Dataframe to list of dicts for DocumentStore
        docs_to_index = [Document(content=row['text'],id=str(row['order'])) for idx, row in data.iterrows()]
        return docs_to_index
    
    def read_stream(self, stream_data):
        # stream consist of single docs for now
        docs_to_index = [Document(content=doc['text'],id=str(doc['id'])) for doc in [stream_data]]
        return docs_to_index


class QARetriever:
    def __init__(self, document_store):
        self.retriever = InMemoryEmbeddingRetriever(document_store=document_store)

    def retrieve(self, query, topk):
        retrieval_results = self.retriever.run(query_embedding=query, top_k=topk)
        documents = [x.to_dict() for x in retrieval_results["documents"]]
        return documents


def rank_aggregation(aggregator, lists, k):
    if aggregator == 'linear':
        csum = Linear.CombSUM(norm='score')
        df_out, df_eval = csum.aggregate(input_file=lists)
    elif aggregator == 'outrank':
        outrank = Majoritarian.OutrankingApproach(eval_pts=7)
        df_out, df_eval = outrank.aggregate(input_file=lists)

    df_out['query_ids'] = df_out.index
    queries = list(df_out['query_ids'].unique())
    results = []
    for query in queries:
        df_query = df_out[df_out['query_ids'] == query][:k]
        rank = 0
        for index, r_q in df_query.iterrows():
            rank += 1
            doc_id = r_q['Voter']
            score_doc = r_q['Score']
            results.append({'qid': query, 'docid': doc_id, 'rank': rank, 'score': score_doc})
    return results


class GenraPipeline:
    #@spaces.GPU(duration=120)
    def __init__(self, llm_instance, emb_model, aggregator, contexts, response_lang):
        self.qa_indexer = QAIndexer('in_memory', emb_model)
        self.qa_retriever = QARetriever(self.qa_indexer.document_store)
        self.encoder = SentenceTransformer(emb_model)
        self.contexts = contexts
        self.aggregator = aggregator
        self.answers_store = {}
        self.llm_model = llm_instance 
        self.llm_generator = LLMGenerator(self.llm_model)
        self.llm_response_lang = response_lang
    
    def retrieval(self, batch_number, queries, topk, summarize_results=True):
        for qid,question in tqdm(queries[['id','query']].values):
            if len(self.contexts)<1:
                self.contexts.append(question)
            all_emb_c = []
            for c in self.contexts:
                c_emb = self.encoder.encode([c], convert_to_numpy=True)[0]
                all_emb_c.append(np.array(c_emb))
            all_emb_c = np.array(all_emb_c)
            avg_emb_c = np.mean(all_emb_c, axis=0)
            avg_emb_c = avg_emb_c.reshape((1, len(avg_emb_c)))
            # we want a list of floats for haystack retrievers
            hits = self.qa_retriever.retrieve(avg_emb_c[0].tolist(), 20) # topk or more?
            hyde_texts = []
            candidate_texts = []
            hit_count = 0
            while len(candidate_texts) < 5:
                if hit_count < len(hits):
                    json_doc = hits[hit_count]
                    doc_text = json_doc['content'] 
                    if self.llm_generator.generate_answer([doc_text], question, self.llm_response_lang, mode='validate'):
                        candidate_texts.append(doc_text) #candidate_texts.append(doc_text[0])
                    hit_count += 1
                else:
                    break
            if len(candidate_texts)<1:
                # no unswerable result
                results = []
            else:
                all_emb_c = []
                all_hits = []
                for i, c in enumerate(candidate_texts):
                    c_emb = self.encoder.encode([c], convert_to_numpy=True)[0]
                    c_emb = c_emb.reshape((1, len(c_emb)))
                    c_hits = self.qa_retriever.retrieve(c_emb[0].tolist(), topk) # changed to len(candidates)+1
                    rank=0
                    for hit in c_hits: # get each ranking with pyflagr format
                        rank += 1
                        # penalize score wrt hit counts (the smaller the better!)
                        all_hits.append({'qid': qid, 'voter':i, 'docid': hit['id'], 'rank': rank, 'score': hit['score']})
                # write pyglagr aggregation files
                tempfile = os.path.join(ROOT_DIR, 'temp', 'temp_rankings_file')
                with open(tempfile, 'w') as f:
                    for res in all_hits:
                        f.write(f"{res['qid']},V{res['voter']},{res['docid']},{res['score']},test\n")
                # run aggregation
                results = rank_aggregation(self.aggregator, tempfile, topk)

                # enhance each result with doc info
                for res in results:
                    res['document'] = self.qa_indexer.document_store.filter_documents(filters={"field": "id", "operator": "==", "value": f"{res['docid']}"})[0].content 

            if summarize_results:
                summary = self.summarize_results(question, results, candidate_texts)

            self.store_results(batch_number, question, results, summary)

    def store_results(self, batch_number, question, results, summary):
        if results:
            tweets = [(t['document'],t['score']) for t in results]
            if question in self.answers_store:
                self.answers_store[question].append({'batch_number':batch_number, 'tweets':tweets, 'summary':summary})
            else:
                self.answers_store[question] = [{'batch_number':batch_number, 'tweets':tweets, 'summary':summary}]
                  
    def summarize_results(self, question, results, candidate_texts):
        if results:
            texts = [t['document'] for t in results] #+ candidate_texts
            summary = self.llm_generator.generate_answer(texts, question, self.llm_response_lang, mode='summarize')
        else:
            summary = "N/A"
        return summary

    def summarize_history(self, queries):
        h_per_q = []
        for qid,question in tqdm(queries[['id','query']].values):
            if question in self.answers_store:
                q_history = self.answers_store[question]
                q_hist_docs = self.order_history(q_history)
                h_per_q.extend(q_hist_docs)
        historical_summary = self.llm_generator.generate_answer(h_per_q, question, self.llm_response_lang, mode='h_summarize')
        return historical_summary
    
    def summarize_no_query(self, texts):
        historical_summary = self.llm_generator.generate_answer(texts, "", self.llm_response_lang, mode='h_summarize')
        return historical_summary
    
    def order_history(self, query_history):
        ordered_history = sorted(query_history, key=itemgetter('batch_number'))
        ordered_docs = [hist['summary'] for hist in ordered_history]
        return ordered_docs


