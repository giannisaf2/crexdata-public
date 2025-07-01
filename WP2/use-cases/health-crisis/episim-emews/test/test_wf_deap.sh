# Base folder
EMEWS_PROJECT_ROOT=$( cd $( dirname $0 )/.. ; /bin/pwd )

# Path to required files
DATA_FOLDER="${EMEWS_PROJECT_ROOT}/data/mitma"
EPISIM_CONFIG="${DATA_FOLDER}/episim_config.json"
WORKFLOW_CONFIG="${DATA_FOLDER}/workflow_settings.json"
DEAP_CONFIG="${DATA_FOLDER}/deap_parameters.json"

# Strategy for the evolutionary algorithm GA/CMA-ES
STRATEGY="deap_cmaes"

# Experiment ID
EXPID="test_${STRATEGY}"

# Machine
MACHINE=linux

# Path to the script to lunch the workflow
COMMAND="${EMEWS_PROJECT_ROOT}/swift/run_wf_deap.sh"

# needed to overwrite the default from the bash lunch scripts
export PROCS=12

# Lunch the workflow!
bash $COMMAND $EXPID $DATA_FOLDER $EPISIM_CONFIG $WORKFLOW_CONFIG $DEAP_CONFIG $STRATEGY $MACHINE