# episim-emews
HPC-based model exploration workflow for epidemic simulations. This project intends to provide an HPC model exploration toolkit. The main use of this project includes:
1) calibration of the model parameters (i.e. finding the values for unknown parameters that minimize a fitting function such as RMSE between the simulation and a time series with real epidemiological data).
2) Optimization-via-simulation of a custom function to design mobility and social distancing policies to control an outbreak or design a vaccination campaign.
3) Characterization of the parameter space running sweeps over a user-defined domain.

# Installion Guide
episim-emews requires different software components and libraries writen in different programming languages. This document will guide you in the steps needed to install all the dependencies and run tests and basic examples.

The first step is to clone this repository:
`git clone https://github.com/Epi-Sim/episim-emews`

## Installing EMEWS
EMEWS is a set of templates based on the [Swift/T](url) HPC scripting language conceived to perform extreme-scale model exploration in HPC environments ([see Documentation]([url](https://swift-lang.github.io/swift-t/sites.html))). Therefore, the main requirement to run EMEWS is installing Swift/T. There are several ways to install Swift/T (see developers' documentation), but the simplest one is using conda.

### Create a conda environment
`conda create -n emews-env`

### Activate your environment
`conda activate emews-env`

### Install gcc compiler
`conda install conda-forge::gcc`

### Install swift
`conda install -c swift-t swift-t`

### Install required python packages
```
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```
### Install Julia Language
Follow the instructions: [Install Julia]([url](https://julialang.org/downloads/))

### Install EpiSim.jl
within the episim-emews folder do the following steps:
```
cd model
bash install.sh
```

### Create a folder to store experiments
`mkdir experiments`

## Running Work-flows
Currently

### Run a sweep test on your computer
The sweep workflow takes as an input the base config file and a list of parameter sets in the following format:
```

bash test/test_wf_sweep.sh
```

# Run a sweep test in your computer
conda activate emews-env
bash test/test_wf_sweep.sh

# Run a deap test in your computer
conda activate emews-env
bash test/test_wf_deap.sh
