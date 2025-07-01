import os
import json
import numpy as np
import pandas as pd
import xarray as xr
import xskillscore as xs


def _aggregate_patches(sim_xa, patch_mapping=None):
    if patch_mapping is None:
        return sim_xa.sum("M")

    if len(sim_xa.dims) == 4:
        dims = ['G', 'M', 'T', 'V']
        sim_xa = sim_xa.transpose(*dims)
        nda_list = []
        for i in patch_mapping.keys():
            nda = sim_xa.loc[:, patch_mapping[i], :].sum("M").values
            nda_list.append(nda)
        data = np.stack(nda_list)
        data = data.transpose(1, 0, 2, 3)
    elif len(sim_xa.dims) == 3:
        dims = ['G', 'M', 'T']
        sim_xa = sim_xa.transpose(*dims)
        nda_list = []
        for i in patch_mapping.keys():
            nda = sim_xa.loc[:, patch_mapping[i], :].sum("M").values
            nda_list.append(nda)
        data = np.stack(nda_list)
        data = data.transpose(1, 0, 2)
    else:
        raise Exception(f"Wrong dimenssion for data array.\
                        Expecting 3 or 4 recieved {len(sim_xa.dims)}")

    new_coords = {}
    for dim in sim_xa.dims:
        new_coords[dim] = sim_xa.coords[dim]

    new_coords["M"] =  list(patch_mapping)
    return xr.DataArray(data=data, coords=new_coords, dims=sim_xa.dims)

def aggregate_patches(sim_ds, instance_folder, data_folder, mapping_fname="rosetta.csv", **kwargs):
    patch_mapping = None
    mapping_fname = os.path.join(data_folder, mapping_fname)
    if os.path.exists(mapping_fname):
        df = pd.read_csv(mapping_fname, dtype=str)
        patch_mapping = {i: df.loc[df["level_2"]==i, "level_1"].tolist() for i in df["level_2"].unique()}


    data_vars = {}
    for var in sim_ds:
        data_vars[var] = _aggregate_patches(sim_ds[var], patch_mapping)

    coords = data_vars[var].coords
    return xr.Dataset(data_vars=data_vars, coords=coords)

def scale_by_population(sim_ds, instance_folder, data_folder, level='prov_age', scale=1e5, **kwargs):

     #The data has to be in xarray.DataSet format
    
    config_fname = os.path.join(instance_folder, "episim_config")
    with open(config_fname) as fh:
        config_dict = json.load(fh)

    G_labels = config_dict['population_params']['G_labels']
    #pop_fname = config_dict['data']["metapopulation_data_filename"]
    pop_fname = os.path.join(data_folder, 'metapopulation_data_prov.csv')
    pop_df = pd.read_csv(pop_fname, dtype={'id':'str'})
    pop_df = pop_df.set_index('id')
    pop_df = pop_df[G_labels].stack()
    pop_df.index.names = ('M', 'G')
    pop_xa = pop_df.to_xarray()
    if level == 'global':
        pop_xa = pop_xa.sum(['M','G'])
        for var in sim_ds:
            sim_ds[var] = sim_ds[var] / pop_xa * scale
        return sim_ds    
    elif level == 'age':
        pop_xa = pop_xa.sum(['M'])
        for var in sim_ds:
            sim_ds[var] = sim_ds[var] / pop_xa * scale
        return sim_ds
    elif level == 'prov':
        pop_xa = pop_xa.sum(['G'])
        for var in sim_ds:
            sim_ds[var] = sim_ds[var] / pop_xa * scale
        return sim_ds
    elif level == 'prov_age':
        for var in sim_ds:
            sim_ds[var] = sim_ds[var] / pop_xa * scale
        return sim_ds
    elif level == 'one_prov':
        region = kwargs['region']
        pop_xa = pop_xa.sel(M=region).drop_vars('M').sum(['G'])
        for var in sim_ds:
            sim_ds[var] = sim_ds[var] / pop_xa * scale
        return sim_ds
    elif level == 'one_prov_age':
        region = kwargs['region']
        pop_xa = pop_xa.sel(M=region).drop_vars('M')
        for var in sim_ds:
            sim_ds[var] = sim_ds[var] / pop_xa * scale
        return sim_ds
    else :
        return "error"


"""
Example of a correct postprocessing function defintion
the function should acept the following arguments
- sim_ds: xarray with simulation output
- instance_folder: string (path to the instance folder)
- data_folder: string (path to the data folder)
- **kwargs: dict (additional arguments as key:value pairs)
"""
def dummy_postprocessing(sim_ds, instance_folder, data_folder, **kwargs):
    return sim_ds


postprocessing_map = {
    "dummy_postprocessing": dummy_postprocessing,
    "scale_by_population": scale_by_population,
    "aggregate_simulation": aggregate_patches
}

def postprocess_obj(instance_folder, data_folder, workflow_config_fname):

    with open(workflow_config_fname) as fh:
        workflow_config = json.load(fh)
    
    postprocess_params = workflow_config.get('postprocessing', None)
    if postprocess_params is None:
        raise Exception("Can't perform postprocess_obj missing key \"postprocessing\" in workflow config")

    output_folder = os.path.join(instance_folder, "output")
    input_fname   = os.path.join(output_folder, postprocess_params["input_fname"])
    output_fname  = os.path.join(output_folder, postprocess_params["output_fname"])
    
    sim_ds = xr.load_dataset(input_fname)    
    for step in postprocess_params['steps']:
        func_name = step['function'] 
        postprocessing_step = postprocessing_map[func_name]
        sim_ds = postprocessing_step(sim_ds, instance_folder, data_folder, **step)

    if postprocess_params["remove_input"]:
        os.remove(input_fname)
    
    sim_ds.to_netcdf(output_fname)
    
    return output_fname
 




