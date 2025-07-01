import io;
import sys;
import files;
import string;
import assert;
import location;

import EQPy;
import lib_settings;
import lib_utils;

string resident_ranks = getenv("RESIDENT_WORK_RANKS");
string r_ranks[]      = split(resident_ranks,",");

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
  assert(population>0, "ERROR: population size must be > 1") =>
  assert(iterations>0, "ERROR: the number of iterations must be > 1") =>
  assert(sigma>0, "ERROR: sigma must be > 1") =>
  assert(file_exists(ea_params_fname), "ERROR: ME param file %s cannot be found" % ea_params_fname) =>
  assert(strlen(me_algo)>0, "ERROR: strategy parameter should be either GA/CMA");
  o = propagate();
}


(void v) loop (location ME) {
  for (boolean b = true, int me_iter = 1;
       b; 
       b=c, me_iter = me_iter + 1)
  {
    // gets the model parameters from the python algorithm
    string params =  EQPy_get(ME);
    boolean c;
    if (params == "DONE") {
        string finals =  EQPy_get(ME);
        string fname = "%s/final_result" % (turbine_output);
        file results_file <fname> = write(finals) =>
        printf("- Writing final result to %s", fname) =>
        v = make_void() =>
        c = false;
    }
    else if (params == "EQPY_ABORT") {
        printf("EQPy Aborted");
        string why = EQPy_get(ME);
        printf("%s", why) =>
        v = propagate() =>
        c = false;
    }
    else {
        string param_array[] = split(params, ";");
        string results[];
        foreach i_params, param_iter in param_array {
          string instance_dir   = "%s/instance_%i_%i" % (turbine_output, me_iter, param_iter) =>
          // call the functions that will do the magic (create folder, run model, postprocess, collect results)
          results[param_iter] = run_obj(instance_dir, base_config, i_params);
        }
        string result = join(results, ";");
        EQPy_put(ME, result) => c = true;
    }
  }
}


(void o) start (int ME_rank, string me_pkg, string algo_params) {
    location ME = locationFromRank(ME_rank);
    printf("- Init ME package %s" % (me_pkg)); 
    EQPy_init_package(ME, me_pkg) =>
    EQPy_get(ME) =>
    printf("- Starting ME!") =>
    EQPy_put(ME, algo_params) =>
      loop(ME) => {
        EQPy_stop(ME);
        o = propagate();
    }
}

//===================================
// WORFLOW STARTS HERE
//===================================
  
// Get path to the base config json file
string base_config = argv("c");
string workflow_path = argv("w");

// Random seed to init ME algorithm
int seed = toint(argv("seed", "0"));

string ea_params_fname = argv("ea_params");
string me_algo = argv("me_algo");
int population = toint(argv("np", "5"));
int iterations = toint(argv("ni","10"));
int sigma      = toint(argv("sigma","3"));
int num_objs   = toint(argv("nobjs", "1"));

printf("=================================================") =>
printf("- Starting MMCA-EMEWS EQ-PY workflow") =>
// Checking all requirments 
check_requirements() => {
  
  if ((me_algo != "deap_ga") && (me_algo != "deap_cmaes")){    
    printf("ERROR: incorrect strategy: %s" % (me_algo));
    printf("ERROR: strategy parameter should be either deap_ga/deap_cma");
  }
  
  printf("=================================================") =>
  printf("- Runtime Info:") =>
  printf("  . EMEWS root: %s" % (emews_root)) =>
  printf("  . TURBINE output: %s" % (turbine_output)) =>
  printf("  . DEBUGG mode: %s" % (debug_mode)) =>
  printf("- MMCA-Epidemic Info:") =>
  printf("  . PATH to config.json: %s" % (base_config)) =>
  printf("  . PATH to data: %s" % (data_path)) =>
  printf("- Model Exploration Info:") =>
  printf("  . ME algorithm: %s" % (me_algo)) =>
  printf("  . ME parameters path: %s" % (ea_params_fname)) =>
  printf("  . ME population size: %i" % (population)) =>
  printf("  . ME num. of iterations: %i" % (iterations)) =>
  printf("  . ME sigma: %i" % (sigma)) =>
  printf("=================================================") =>
  printf("- Beginning Model exploration!") =>
  
  string me_algo_params;
  if (me_algo == "deap_ga"){
    me_algo_params = "%d,%d,%d,'%s',%d" %  (iterations, population, seed, ea_params_fname, num_objs);
  }
  else if (me_algo == "deap_cmaes"){
    me_algo_params = "%d,%d,%d,%d,'%s',%d" %  (iterations, population, sigma, seed, ea_params_fname, num_objs);
  }
  int rank = string2int(r_ranks[0]) =>
  wait(me_algo_params) {
    void done = start(rank, me_algo, me_algo_params);
    wait(done) {
      printf("- Finshed Computation") =>
      collect_metrics(collect_metrics_path);
    }
  }
}