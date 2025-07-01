import os
import sys
import json
import glob
import polars as pl
import importlib
import multiprocessing
from concurrent.futures import ProcessPoolExecutor, as_completed

try:
    import orjson as json_parser
    def load_json(path):
        with open(path, "rb") as f:
            return json_parser.loads(f.read())
except ImportError:
    def load_json(path):
        with open(path, "r") as f:
            return json.load(f)



# Setup module path
base_folder = os.path.realpath(os.path.join(os.path.dirname(sys.argv[0]), ".."))

custom_module_path = os.path.join(base_folder, "python")
sys.path.append(custom_module_path)

custom_module_name = "episim_evaluate"
episim_evaluate = importlib.import_module(custom_module_name)


def process_instance(instance_path, data_folder, wf_config_fname, episim_config):
    path_pieces = instance_path.split("/")
    instance_id = path_pieces[-1]

    split_instance = instance_id.split("_")
    if len(split_instance) == 2:
        gen = 1
        ind = split_instance[1]
    elif len(split_instance) == 3:
        gen = split_instance[1]
        ind = split_instance[2]
    else:
        raise Exception(f"Invalid directory name for instance {instance_id}")

    config_json = os.path.join(instance_path, episim_config)
    config = load_json(config_json)

    epi_params = config.get("epidemic_params", {})
    npi_params = config.get("NPI", {})
    pop_params = config.get("population_params", {})
    G_labels = pop_params.get("G_labels", [])

    row = {"instance_path": instance_path, "gen": gen, "ind": ind}

    for key, value in epi_params.items():
        if isinstance(value, list) and len(value) == len(G_labels):
            for label, v in zip(G_labels, value):
                row[f"{key}{label}"] = v
        else:
            row[key] = value

    for key, value in npi_params.items():
        if isinstance(value, list):
            for i, v in enumerate(value):
                row[f"{key}_{i}"] = v
        else:
            row[key] = value

    workflow_json = os.path.join(data_folder, wf_config_fname)
    cost = episim_evaluate.evaluate_obj(instance_path, data_folder, workflow_json)
    row["cost"] = cost

    return row


def collect_results(experiment_folder, wf_config_fname="workflow_settings.json", episim_config="episim_config.json", cpus=4):
    data_folder = os.path.join(experiment_folder, "data")
    instance_paths = glob.glob(os.path.join(experiment_folder, "instance_*"))

    data_rows = []
    NUM_CPUS = cpus
    with ProcessPoolExecutor(max_workers=NUM_CPUS) as executor:
        futures = [
            executor.submit(process_instance, path, data_folder, wf_config_fname, episim_config)
            for path in instance_paths
        ]
        for future in as_completed(futures):
            try:
                row = future.result()
                data_rows.append(row)
            except Exception as e:
                print(f"Error processing instance: {e}")

    # Create Polars DataFrame
    df = pl.DataFrame(data_rows)

    # Remove constant columns
    df = df.select([col for col in df.columns if df.select(pl.col(col).n_unique()).item() > 1])

    # Sort by cost
    df = df.sort("cost")

    return df


if __name__ == "__main__":
    experiment_folder = sys.argv[1]
    print(f"- Collecting metrics from experiment {experiment_folder}")

    cpus = multiprocessing.cpu_count()
    df = collect_results(experiment_folder, wf_config_fname="workflow_settings.json", cpus=cpus)

    output_name = os.path.join(experiment_folder, "experiment_results.csv")
    print(f"- Writing experiment metrics into {output_name}")
    df.write_csv(output_name)