# This environemnetal variables are required to used the self-contained julia version
BASE_FOLDER="$(realpath "$(dirname "${BASH_SOURCE[0]}")/..")"
export PATH=$PATH:${BASE_FOLDER}/julia/bin
export JULIA_DEPOT_PATH=${BASE_FOLDER}/.julia

log_script() {
  SCRIPT_NAME=$(basename $0)
  mkdir -p $TURBINE_OUTPUT
  LOG_NAME="${TURBINE_OUTPUT}/${SCRIPT_NAME}.log"
  echo "### VARIABLES ###" > $LOG_NAME
  set +u
  VARS=( "EMEWS_PROJECT_ROOT" "EXPID" "TURBINE_OUTPUT" \
    "PROCS" "QUEUE" "WALLTIME" "PPN" "TURBINE_JOBNAME" \
    "PYTHONPATH" "R_HOME" "LD_LIBRARY_PATH" "DYLD_LIBRARY_PATH" \
    "TURBINE_RESIDENT_WORK_WORKERS" "RESIDENT_WORK_RANKS" "EQPY" \
    "EQR" "CMD_LINE_ARGS" "MACHINE")
  for i in "${VARS[@]}"
  do
      v=\$$i
      echo "$i=`eval echo $v`" >> $LOG_NAME
  done

  for i in "${USER_VARS[@]}"
  do
      v=\$$i
      echo "$i=`eval echo $v`" >> $LOG_NAME
  done
  set -u

  echo "" >> $LOG_NAME
  echo "## SCRIPT ###" >> $LOG_NAME
  cat $EMEWS_PROJECT_ROOT/swift/$SCRIPT_NAME >> $LOG_NAME
}

check_directory_exists() {
  if [[ -d $TURBINE_OUTPUT ]]; then
    while true; do
      read -p "Experiment directory exists. Continue? (Y/n) " yn
      yn=${yn:-y}
      case $yn in
          [Yy""]* ) break;;
          [Nn]* ) exit; break;;
          * ) echo "Please answer yes or no.";;
      esac
    done
  fi

}

setup_experiment() {
  
  WORKFLOW_TYPE=$1
  check_directory_exists
  rm -fr "${TURBINE_OUTPUT}"
  mkdir -p ${TURBINE_OUTPUT}

  #################################################################

  if [ ! -d "${BASE_DATA_FOLDER}" ]; then
      echo "Base data folder ${BASE_DATA_FOLDER} doe not exists"
      exit;
  fi

  if [ ! -d "${DATA_FOLDER}" ]; then
      echo "Creating data folder at turbine output"
      mkdir ${DATA_FOLDER}
  fi

  echo "Copying data files into turbine output"
  cp ${BASE_DATA_FOLDER}/* ${DATA_FOLDER}

  #################################################################
  
  if [ ! -f "${BASE_CONFIG_JSON}" ]; then
      echo "Base config file ${BASE_CONFIG_JSON} doe not exists"
      exit;
  fi

  echo "Copying base config file into turbine output"
  cp ${BASE_CONFIG_JSON} ${CONFIG_JSON}
  
  #################################################################
    
  if [ ! -f "${BASE_WORKFLOW_CONFIG}" ]; then
      echo "Workflow config file ${BASE_WORKFLOW_CONFIG} doe not exists"
      exit;
  fi

  echo "Copying workflow config file into turbine output"
  cp ${BASE_WORKFLOW_CONFIG} ${WORKFLOW_CONFIG}
  
  #################################################################

  if [ "${WORKFLOW_TYPE}" = "SWEEP" ]; then
      if [ ! -f "${BASE_PARAMS_SWEEP}" ]; then
        echo "Sweep file ${BASE_PARAMS_SWEEP} does not exist"
        exit;
      else
        echo "Copying base params file into turbine output"
        cp ${BASE_PARAMS_SWEEP} ${PARAMS_SWEEP}
      fi
  elif [ "${WORKFLOW_TYPE}" = "DEAP" ]; then
      if [ ! -f "${BASE_PARAMS_DEAP}" ]; then
        echo "Sweep file ${BASE_PARAMS_DEAP} does not exist"
        exit;
      fi
      echo "Copying base deap params file into turbine output"
      cp ${BASE_PARAMS_DEAP} ${PARAMS_DEAP}
  else
      echo "Unknown workflow type ${WORKFLOW_TYPE}"
      exit;
  fi

}
