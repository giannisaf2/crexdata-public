#! /usr/bin/env bash
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

# uncomment to turn on swift/t logging. Can also set TURBINE_LOG,
# TURBINE_DEBUG, and ADLB_DEBUG to 0 to turn off logging
# export TURBINE_LOG=1 TURBINE_DEBUG=1 ADLB_DEBUG=1
export DEBUG_MODE=2

export EMEWS_PROJECT_ROOT="$(realpath "$(dirname "${BASH_SOURCE[0]}")/..")"
export PYTHONPATH="${PYTHONPATH}:${EMEWS_PROJECT_ROOT}/python"


# source some utility functions used by EMEWS in this script
source "${EMEWS_PROJECT_ROOT}/etc/emews_utils.sh"

set -eu

if [ "$#" -ne 6 ]; then
  script_name=$(basename $0)
  echo "Usage: ${script_name} EXPERIMENT_ID (e.g. ${script_name} experiment_1) DATA_FOLDER CONFIG_JSON WOKFLOW_JSON PARAMS_SWEEP CLUSTER_NAME (mn5/nord3/local)"
  exit 1
fi

EXPID=$1
export TURBINE_OUTPUT="${EMEWS_PROJECT_ROOT}/experiments/${EXPID}"

BASE_DATA_FOLDER=$2
DATA_FOLDER="${TURBINE_OUTPUT}/data"

BASE_CONFIG_JSON=$3
CONFIG_JSON="${TURBINE_OUTPUT}/episim_config.json"

BASE_WORKFLOW_CONFIG=$4
WORKFLOW_CONFIG="${TURBINE_OUTPUT}/workflow_settings.json"

BASE_PARAMS_SWEEP=$5
PARAMS_SWEEP="${TURBINE_OUTPUT}/sweep_params.txt"

#################################################################
CLUSTER_NAME=$6

# This will load all the required env variables
source "${EMEWS_PROJECT_ROOT}/etc/cluster_settings.sh"

if [ -n "$LOAD_MODULES" ]; then
  echo "Loading required module: $LOAD_MODULES"
  eval $LOAD_MODULES
fi

source $EMEWS_PROJECT_ROOT/venv/bin/activate

#################################################################
# function that check all files exist and creates the experiment 
# folder ($TURBINE_OUTPUT) and copy all files required by the
# experiments

WORKFLOW_TYPE="SWEEP"
setup_experiment $WORKFLOW_TYPE

#################################################################
# Computing Resources

export PROCS=12
export PPN=$PROCS
export PROJECT=${ACCOUNT}
export WALLTIME=02:00:00

export TURBINE_JOBNAME="${EXPID}_job"

#################################################################

# log variables and script to to TURBINE_OUTPUT directory
log_script
# echo's anything following this standard out
set -x

#################################################################

# Swift custom libraries
SWIFT_PATH="${EMEWS_PROJECT_ROOT}/swift"

# Swift workflow script
SWIFT_WF="${SWIFT_PATH}/run_wf_sweep.swift"

# Command line arguments for swift workflow
WF_ARGS="-d=${DATA_FOLDER} -c=${CONFIG_JSON} -w=${WORKFLOW_CONFIG} -f=${PARAMS_SWEEP}"


if [ "$MACHINE" == "slurm" ]; then
  export TURBINE_LAUNCHER="srun"
  if [ -n ${QUEUE} ]; then
    export TURBINE_SBATCH_ARGS="--qos=${QUEUE}"
  fi
  swift-t -p -n $PROCS -m $MACHINE -I $SWIFT_PATH  $SWIFT_WF  $WF_ARGS
else
  swift-t -p -n $PROCS -I $SWIFT_PATH  $SWIFT_WF  $WF_ARGS
fi



