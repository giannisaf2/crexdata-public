CLUSTER_NAME="${CLUSTER_NAME:-linux}"
MEM="${MEM:-}"
# Handle different cluster configurations
if [ "$CLUSTER_NAME" == "mn5" ]; then
    MACHINE="slurm"
    # Setting required modules for MN5
    LOAD_MODULES="module load swig java-jdk/8u131 ant/1.10.14 R/4.3.2 zsh hdf5 python/3.12.1 swiftt/1.6.2-python-3.12.1"
    # Setting ACCOUNT and QUEUE for MN5
    ACCOUNT="bsc08"
    QUEUE="gp_debug" # gp_bscls
    PPN=112
elif [ "$CLUSTER_NAME" == "nord4" ]; then
    MACHINE="slurm"
    # Setting required modules for MN5
    LOAD_MODULES="module load zsh hdf5 java/8u131 R/3.4.0 python/3.12.0 swiftt/1.5.0"
    # Setting ACCOUNT and QUEUE for Nord4
    ACCOUNT="bsc08"
    QUEUE="bsc_ls"
    PPN=48
elif [ "$CLUSTER_NAME" == "elastic" ]; then
    MACHINE="slurm"
    # Clear LOAD_MODULES for elastic cluster
    LOAD_MODULES=""
    # Define memory for elastic cluster (no ACCOUNT or QUEUE needed)
    MEM="14G"
    # Clear ACCOUNT and QUEUE to trigger the memory-based allocation
    ACCOUNT=""
    QUEUE=""
    PPN=8
else
    MACHINE="linux"
    # Clear LOAD_MODULES for single linux machine
    LOAD_MODULES=""
    # Clear ACCOUNT and QUEUE for linux single machine
    ACCOUNT=""
    QUEUE=""
    PPN=12
fi

# Echo the configuration for debugging purposes
echo "=== Cluster Settings ==="
echo " - Cluster name: $CLUSTER_NAME"
echo " - Machine type: $MACHINE"
[ -n "$ACCOUNT" ] && echo " - Account: $ACCOUNT"
[ -n "$QUEUE" ] && echo " - Queue: $QUEUE"
[ -n "$MEM" ] && echo " - Memory: $MEM"
echo "===================="