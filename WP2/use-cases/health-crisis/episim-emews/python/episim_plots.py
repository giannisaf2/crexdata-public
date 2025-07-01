import os
import json

import xarray as xr
import numpy as np
import pandas as pd

import matplotlib.pyplot as plt
import matplotlib.dates as mdates

import episim_utils

def plot_epivariables(
                        sim_ds,
                        instance_folder,
                        data_folder,
                        epivar='new_deaths',
                        agg_level="level_2",
                        obsdata_fname="real_observables.nc",
                        smooth_obs=False,
                        scale_by_pop=True,
                        epi_variables = ['new_infected', 'new_hospitalized', 'new_deaths'],
                        excluded_regions = ["51", "52"]
                        ):
    
    config_fname = os.path.join(instance_folder, "episim_config.json")
    with open(config_fname) as fh:
        config_dict = json.load(fh)

    start_date = config_dict['simulation']['start_date']
    end_date = config_dict['simulation']['end_date']

    if epivar not in epi_variables:
        raise ValueError(f"Unsupported epivar: {epivar}. Use: ", epi_variables)

    obs_path = os.path.join(data_folder, obsdata_fname)
    obs_ds = xr.load_dataset(obs_path)

    # Apply optional smoothing (7-day rolling mean)
    if smooth_obs:
        obs_ds = obs_ds.rolling(T=7, center=True, min_periods=1).mean()

    # Ensure datetime types and clip to simulation period
    sim_ds['T'] = pd.to_datetime(sim_ds['T'].values)
    sim_ds = sim_ds.sel(T=slice(start_date, end_date))

    obs_ds['T'] = pd.to_datetime(obs_ds['T'].values)
    obs_ds = obs_ds.sel(T=slice(start_date, end_date))
    
    # Keep only relevant variables
    sim_ds = sim_ds[epi_variables]
    obs_ds = obs_ds[epi_variables]

    # Align time and space
    obs_ds, sim_ds = xr.align(obs_ds, sim_ds)

    # Load population data
    pop_fname = config_dict["data"]["metapopulation_data_filename"]
    metapop_csv = os.path.join(data_folder, pop_fname)
    rosetta_csv = os.path.join(data_folder, "rosetta.csv")
    metapop = episim_utils.Metapopulation(metapop_csv, rosetta_csv=rosetta_csv)
    pop_da = metapop.aggregate_to_level(agg_level)

    regions = pop_da["M"].values
    regions = sorted([i for i in regions if i not in excluded_regions])
    n_regions = len(regions)

    # Optionally scale values by population
    if scale_by_pop:
        obs_ds = obs_ds / pop_da * 1e5
        sim_ds = sim_ds / pop_da * 1e5

    df_obs = obs_ds[epivar].sum("G").to_dataframe().reset_index()
    df_obs = df_obs[~df_obs["M"].isin(excluded_regions)]

    df_sim = sim_ds[epivar].sum("G").to_dataframe().reset_index()
    df_sim = df_sim[~df_sim["M"].isin(excluded_regions)]

    # Layout
    ncols = 10
    nrows = int(np.ceil(n_regions / ncols))

    fig, axes = plt.subplots(nrows=nrows, ncols=ncols, figsize=(ncols * 2, nrows * 1.5), sharex=True, sharey=False)
    axes_f = axes.flatten()

    for idx, region in enumerate(regions):
        ax = axes_f[idx]
        
        data = df_sim[df_sim["M"] == region]
        ax.plot(data["T"], data[epivar], linestyle="--", color="black", linewidth=2.5, label="Simulation")

        data = df_obs[df_sim["M"] == region]
        ax.plot(data["T"], data[epivar], "o-", color="grey", linewidth=0.5, label="Observation", markersize=1.5)

        ax.set_title(f"Region {region}", fontsize=9)

        ax.spines['top'].set_visible(False)
        ax.spines['right'].set_visible(False)

        if idx % ncols == 0:  # first column
            ax.set_ylabel(epivar.replace("_", " ").title())
        else:
            ax.set_ylabel("")

    # Remove unused axes
    for j in range(idx + 1, len(axes_f)):
        fig.delaxes(axes_f[j])
    
    for ax in axes[-1, :]:
        ax.xaxis.set_major_locator(mdates.MonthLocator())
        ax.xaxis.set_major_formatter(mdates.DateFormatter("%b"))
        ax.tick_params(axis="x", labelbottom=True)

    fig.tight_layout(rect=[0, 0.0, 1, 0.95])  # Leave space for bottom legend

    # Global legend with 2 columns at the bottom
    handles, labels = axes_f[0].get_legend_handles_labels()
    fig.legend(handles, labels, loc='lower center', ncol=2, bbox_to_anchor=(0.5, 0.94))
    
    return fig




plot_function_map = {
    "plot_epivariables": plot_epivariables
}

def plot_obj(instance_folder, data_folder, workflow_config_fname):

    with open(workflow_config_fname) as fh:
        workflow_config = json.load(fh)
    
    wf_plot_params = workflow_config.get("plot", None)
    if wf_plot_params is None:
        return 0

    output_folder = os.path.join(instance_folder, "output")
    input_fname   = os.path.join(output_folder, wf_plot_params["input_fname"])
    sim_ds = xr.load_dataset(input_fname)

    if "figures" not in wf_plot_params:
        print(f"Warning. No keyword figure found in \"plot\" entry of {workflow_config_fname}")
        return 0
    
    total_figs = 0
    for fig_step in wf_plot_params["figures"]:
        plot_func_name = fig_step["function"] 
        params = fig_step["parameters"]
        fig_name = fig_step["fig_name"]
        
        plot = plot_function_map[plot_func_name]
        fig_name = os.path.join(output_folder, fig_name)
        fig = plot(sim_ds, instance_folder, data_folder, **params)
        fig.savefig(fig_name)
        plt.close(fig)
        total_figs += 1

    return total_figs

    
    
    