import io;
import sys;
import files;
import assert;
import python;
import string;
import random;
import lib_settings;

//===================================================
// AUXILIARY FUNCTIONS TO RUN COMMANDS IN THE SHELL
//===================================================

// deletes the specified directory
app (void o) delete_file(string fname) {
  "rm" fname;
}

// deletes the specified directory
app (void o) data_cleanup(string pattern) {
  "rm" "-rf" pattern;
}

app (void o) copy_file(string source, string destination) {
  "cp" "-rf" source destination;
}

// call this to create any required directories
app (void o) make_dir(string dirname) {
  "mkdir" "-p" dirname;
}

// call the shell script to executes the model
app (file out, file err) run_model (string instance, string config) {
    "bash" model_sh model_exec data_path instance config @stdout=out @stderr=err;
}

app (void o) collect_metrics(string script_path) {
  "python" script_path turbine_output;
}

//========================================
// FUNCTION TO EVALUATE A MODEL INSTANCE
//========================================

(string result) run_obj(string instance, string base_conf, string parameters) {
  make_dir(instance) => {
    string config_out = instance + "/" + base_config_name;
    create_config(base_conf, config_out, parameters) =>  {
      file out <instance + "/out.txt">;
      file err <instance + "/err.txt">;
      (out, err) = run_model(instance, config_out) => {
        string output_fname = postprocess_obj(instance, data_path, workflow_path) =>
        result = get_result(instance, data_path, workflow_path);
      }
    }
  }
}

//===================================
// FUNCTION FOR UPDATING PARAMETERS
//===================================

string update_json_template = """
import json
import episim_utils
config_base_fname  = '%s'
config_out_fname   = '%s'
params_strn        = '%s'

params_strn = params_strn.replace("'", '"')
param_dict = json.loads(params_strn)

episim_config = episim_utils.EpiSimConfig.from_json(config_base_fname)
episim_config.update_params_from_flat_dict(param_dict)
episim_config.to_json(config_out_fname)
""";
(void o) create_config(string cfg_base, string cfg_out, string strn_params) {
    string code = update_json_template % (cfg_base, cfg_out, strn_params);
    python_persist(code, "'ignore'") =>
    o = propagate();
}


//===================================
// FUNCTION FOR POST-PROCESSING
//===================================

string postprocess_obj_template = """
import os
import json
import episim_postprocessing
import episim_plots

instance_folder = '%s'
data_folder     = '%s'
wf_config_fname = '%s'

output_fname = episim_postprocessing.postprocess_obj(instance_folder, data_folder, wf_config_fname)
total_figs = episim_plots.plot_obj(instance_folder, data_folder, wf_config_fname)
""";
(string output_fname) postprocess_obj(string instance, string data_folder, string wf_cfg) {
 string py_code = postprocess_obj_template % (instance, data_folder, wf_cfg);
 output_fname = python_persist(py_code, "str(output_fname)");
}

// ===================================
// FUNCTION FOR RETRIEVING RESULTS
// ===================================

string read_result_code ="""
import os
import json
import episim_evaluate

instance_folder = '%s'
data_folder     = '%s'
workflow_json   = '%s'

result = episim_evaluate.evaluate_obj(instance_folder, data_folder, workflow_json)
""";
(string result) get_result(string instance, string data_folder, string wf_cfg) {
  string code = read_result_code % (instance, data_folder, wf_cfg);
  result = python_persist(code, "str(result)");
}


//==================
// UTIL FUNCTIONS
//==================

string date_conversor ="""
from datetime import datetime
from datetime import timedelta

strn_date = "%s"
delta = %i
date_format = "%%Y-%%m-%%d"

date_start = datetime.strptime(strn_date, date_format)
date_end = date_start + timedelta(days=delta)
result = date_end.strftime(date_format)
""";
(string result) get_date(string strn_date, int delta) 
{
  string p_code = date_conversor % (strn_date, delta);
  result = python_persist(p_code, "str(result)");
}


