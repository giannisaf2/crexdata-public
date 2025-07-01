import threading
import random
import time
import math
import csv
import json
import sys
import time
import pickle

import numpy as np

from deap import base
from deap import creator
from deap import tools
from deap import algorithms

import eqpy, deap_utils

# list of ga_utils parameter objects
transformer = None

class Transformer:

    def __init__(self, ea_params, clf = None, scaler = None):
        self.ea_params = ea_params

    def mutate(self, population, indpb):
        """
        Mutates the values in list individual with probability indpb
        """

        # Note, if we had some aggregate constraint on the individual
        # (e.g. individual[1] * individual[2] < 10), we could copy
        # individual into a temporary list and mutate though until the
        # constraint was satisfied
        for i, param in enumerate(self.ea_params):
            individual = param.mutate(population[i], mu=0, indpb=indpb)          
            population[i] = individual

        return population,

    def cxUniform(self, ind1, ind2, indpb):
        for _ in range(100):
            c1, c2 = tools.cxUniform(ind1, ind2, indpb)

        return (c1, c2)

    def random_params(self):
        draws = []
        for p in self.ea_params:
            draws.append(p.randomDraw())

        return draws

    def parse_init_params(self, params_file):
        init_params = []
        with open(params_file) as f_in:
            reader = csv.reader(f_in)
            header = next(reader)
            for row in reader:
                init_params.append(dict(zip(header,row)))
        return init_params


def printf(val):
    print(val)
    sys.stdout.flush()

def obj_func(x):
    return 0

def create_list_of_json_strings(list_of_lists, super_delim=";"):
    # create string of ; separated jsonified maps
    res = []
    global transformer
    for l in list_of_lists:
        jmap = {}
        for i,p in enumerate(transformer.ea_params):
            jmap[p.name] = l[i]

        jstring = json.dumps(jmap)
        res.append(jstring)

    return (super_delim.join(res))

def queue_map(obj_func, pops):
    # Note that the obj_func is not used
    # sending data that looks like:
    # [[a,b,c,d],[e,f,g,h],...]
    
    if not pops:
        return []

    eqpy.OUT_put(create_list_of_json_strings(pops))
    result = eqpy.IN_get()
    split_result = result.split(';')

    fitness_list = []
    for x in split_result:
        xs = x.split(",")
        fitness_vals = []
        for i,v in enumerate(xs):
            if math.isnan(float(v)):
                v = 9999999999.0 
            fitness_vals.append(float(v))
        fitness_list.append(tuple(fitness_vals))
    
    return fitness_list


def make_random_parameters():
    """
    Performs initial random draw on each parameter
    """
    return transformer.random_params()

# Returns a tuple of one individual
def custom_mutate(individual, indpb):
    """
    Mutates the values in list individual with probability indpb
    """
    return transformer.mutate(individual, indpb)

def cxUniform(ind1, ind2, indpb):
    return transformer.cxUniform(ind1, ind2, indpb)

def timestamp(scores):
    return str(time.time())


def run():
    """
    :param num_iterations: number of generations
    :param seed: random seed
    :param ga parameters file name: ga parameters file name (e.g., "ea_params.json")
    :param num_population population of ga algorithm
    """
    eqpy.OUT_put("Params")
    parameters = eqpy.IN_get()

    # parse params
    printf("Parameters: {}".format(parameters))
    (num_iterations, num_population, seed, ea_parameters_file, num_objectives) = eval('{}'.format(parameters))
    random.seed(seed)
    ea_parameters = deap_utils.create_parameters(ea_parameters_file)
    global transformer
    transformer = Transformer(ea_parameters)

    weights = tuple([-1] * int(num_objectives))

    # deap class creators
    creator.create("FitnessMulti", base.Fitness, weights=weights)
    creator.create("Individual", list, fitness=creator.FitnessMulti)

    # deap method definitions
    toolbox = base.Toolbox()
    toolbox.register("individual", tools.initIterate, creator.Individual,
                     make_random_parameters)

    toolbox.register("population", tools.initRepeat, list, toolbox.individual)
    toolbox.register("evaluate", obj_func)
    toolbox.register("mate", cxUniform, indpb=0.5)
    toolbox.register("mutate", custom_mutate, indpb=0.2)
    if len(weights) == 1:
        toolbox.register("select", tools.selTournament, tournsize=3)
    else:
        toolbox.register("select", tools.selNSGA2)
    
    toolbox.register("map", queue_map)

    pop = toolbox.population(n=num_population)   

    hof = tools.HallOfFame(1)
    stats = tools.Statistics(lambda ind: ind.fitness.values)
    stats.register("avg", np.mean)
    stats.register("std", np.std)
    stats.register("min", np.min)
    stats.register("max", np.max)
    stats.register("ts", timestamp)

    # num_iter-1 generations since the initial population is evaluated once first
    start_time = time.time()
    pop, log = algorithms.eaSimple(pop, toolbox, cxpb=0.5, mutpb=0.2, ngen=num_iterations - 1, stats=stats, halloffame=hof, verbose=True)
    end_time = time.time()

    fitnesses = [str(p.fitness.values[0]) for p in pop]

    eqpy.OUT_put("DONE")
    
    # return the best individual
    eqpy.OUT_put(create_list_of_json_strings([hof[0]]))
    
    # eqpy.OUT_put("{}\n{}\n{}\n{}\n{}".format(create_list_of_json_strings(pop), ';'.join(fitnesses),
    #    start_time, log, end_time))