/*
 * EpiSim-EMEWS Parameter Sweep Workflow
 * -------------------------------------
 * This script reads a set of parameter configurations from a .txt file
 * and runs them in parallel using Swift/T and the EMEWS framework.
 *
 * Required arguments:
 *   -c : Path to base config JSON
 *   -w : Path to workflow config JSON
 *   -f : Path to parameter sets file (.txt)
 *
 * Author: Miguel Ponce-de-Leon
 * Date: 2025-06-05
 */

import io;
import sys;
import files;
import string;
import assert;
import lib_utils;
import lib_settings;

//===================================
// FUNCTION FOR CHECKING REQUIRMENTS
//===================================

(void o) check_requirements() {
  printf(" - Checking requirements:") =>
  assert(strlen(emews_root) > 0, "Set EMEWS_PROJECT_ROOT!") =>
  assert(strlen(getenv("PYTHONPATH")) > 0, "Set PYTHONPATH!") =>
  printf("  . Checking config: %s" % (base_config)) => 
  assert(file_exists(base_config), "Base config file does not exists");
  printf("  . Checking the required data: %s" % (data_path)) =>
  assert(file_exists(data_path), "Data folder does not exists") =>
  printf("  . Checking workflow config: %s" % (workflow_path)) =>
  assert(file_exists(workflow_path), "Workflow config does not exists") =>
  o = propagate();
}

// Get path to the base config json file
string base_config = argv("c");
string workflow_path = argv("w");

// Get .txt files having the parameters to evaluate
string params_fname = argv("f");

printf("=================================================") =>
printf("- Starting EpiSim-EMEWS parameter sweep workflow") =>
// Checking all requirments 
check_requirements() => {
  
  printf("=================================================") =>
  printf("- Runtime Info:") =>
  printf("  . EMEWS root: %s" % (emews_root)) =>
  printf("  . TURBINE output: %s" % (turbine_output)) =>
  printf("  . DEBUGG mode: %s" % (debug_mode)) =>
  printf("- EpiSim-Epidemic Info:") =>
  printf("  . PATH to config.json: %s" % (base_config)) =>
  printf("  . PATH to data: %s" % (data_path)) =>
  printf("  . PATH to workflow json: %s" % (workflow_path)) =>
  printf("  . PATH to parameters.txt: %s" % (params_fname)) =>
  
  // Reading file containing the list of parameters to evaluate
  file upf = input(params_fname) =>
  // Iterating over the list of parameters sets
  
  string upf_lines[] = file_lines(upf) =>
  printf("=================================================") =>
  printf("- Beginning parallel sweep!") => 
  printf("  . Evaluating %i parameter sets" % (size(upf_lines))) => {

    string results[];
    foreach string_params,i in upf_lines {
      // define the instance path
      string instance_dir = "%s/instance_%i" % (turbine_output, i+1) =>
      // call the functions that will do the magic (create folder, run model, postprocess, collect results)
      results[i] = run_obj(instance_dir, base_config, string_params);
    }
    wait (results) {
      collect_metrics(collect_metrics_path);
    }
  }
}
