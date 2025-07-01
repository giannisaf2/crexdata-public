# Base folder
EMEWS_PROJECT_ROOT=$( cd $( dirname $0 )/.. ; /bin/pwd )

# Path to required files
DATA_FOLDER="${EMEWS_PROJECT_ROOT}/data/mitma"
EPISIM_CONFIG="${DATA_FOLDER}/episim_config.json"
WORKFLOW_CONFIG="${DATA_FOLDER}/workflow_settings.json"
PARAMS_SWEEP="${DATA_FOLDER}/sweep_params_n10.txt"

# Experiment ID
EXPID="test_sweep"

# Machine
MACHINE=linux

# Path to the script to lunch the workflow
COMMAND="${EMEWS_PROJECT_ROOT}/swift/run_wf_sweep.sh"

# needed to overwrite the default from the bash lunch scripts
export PROCS=10

# Lunch the workflow!
bash $COMMAND $EXPID $DATA_FOLDER $EPISIM_CONFIG $WORKFLOW_CONFIG $PARAMS_SWEEP $MACHINE