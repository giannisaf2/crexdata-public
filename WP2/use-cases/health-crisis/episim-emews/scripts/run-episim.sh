#!/bin/bash

set -eu

USE_COMPILED_EPISIM=true

# Check for an optional timeout threshold in seconds. If the duration of the
# model run as executed below, takes longer that this threshhold
# then the run will be aborted. Note that the "timeout" command
# must be supported by executing OS.

# The timeout argument is optional. By default the "run_model" swift
# app fuction sends 3 arguments, and no timeout value is set. If there
# is a 4th (the TIMEOUT_ARG_INDEX) argument, we use that as the timeout value.

# !!! IF YOU CHANGE THE NUMBER OF ARGUMENTS PASSED TO THIS SCRIPT, YOU MUST
# CHANGE THE TIMEOUT_ARG_INDEX !!!
TIMEOUT=""
TIMEOUT_ARG_INDEX=5
if [[ $# ==  $TIMEOUT_ARG_INDEX ]]
then
	TIMEOUT=${!TIMEOUT_ARG_INDEX}
fi

TIMEOUT_CMD=""
if [ -n "$TIMEOUT" ]; then
  TIMEOUT_CMD="timeout $TIMEOUT"
fi

# Set param_line from the first argument to this script
# param_line is the string containing the model parameters for a run.
executable_path=$(realpath ${1})

# Set emews_root to the root directory of the project (i.e. the directory
# that contains the scripts, swift, etc. directories and files)

data_path=$(realpath ${2})
# Each model run, runs in its own "instance" directory
# Set instance_directory to that and cd into it.
instance_directory=$(realpath ${3})
cd $instance_directory
mkdir -p output

# Get configuration path (config.json)
config_path=$(realpath ${4})

# TODO: Define the command to run the model. For example,
if $USE_COMPILED_EPISIM
then
    MODEL_CMD="${executable_path}"
else
    MODEL_BASH_PATH="$(dirname ${executable_path})"
    MODEL_BASH_PATH="$(realpath ${MODEL_BASH_PATH}/../)"
    MODEL_CMD="julia  ${MODEL_BASH_PATH}/model/EpiSim.jl/src/run.jl"
fi

arg_array=("run" "-d $data_path" "-c $config_path" "-i $instance_directory")
COMMAND="$MODEL_CMD ${arg_array[@]}"

echo $COMMAND > /tmp/debug
# Turn bash error checking off. This is
# required to properly handle the model execution return value
# the optional timeout.
echo "READY"
set +e
echo "$COMMAND"


# $TIMEOUT_CMD $COMMAND
$COMMAND
# $? is the exit status of the most recently executed command (i.e the
# line above)
RES=$?
if [ "$RES" -ne 0 ]; then
	if [ "$RES" == 124 ]; then
    echo "---> Timeout error in $COMMAND"
  else
	   echo "---> Error in $COMMAND"
  fi
fi
