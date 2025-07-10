# FDA-Opt: A Flower / HuggingFace app
System implementation for the [CREXDATA](https://crexdata.eu/) Project of
```bibtex
@misc{theologitis2025communication,
    title={Communication-Efficient Federated Fine-Tuning of Language Models via Dynamic Update Schedules},
    author={Michail Theologitis and Vasilis Samoladas and Antonios Deligiannakis},
    year={2025},
    eprint={2505.04535},
    archivePrefix={arXiv},
    primaryClass={cs.LG}
}
```
building upon the work of
```bibtex
@inproceedings{theologitis2025fda,
  author       = {Michail Theologitis and
                  Georgios Frangias and
                  Georgios Anestis and
                  Vasilis Samoladas and
                  Antonios Deligiannakis},
  title        = {Communication-Efficient Distributed Deep Learning via Federated Dynamic
                  Averaging},
  booktitle    = {Proceedings 28th International Conference on Extending Database Technology,
                  {EDBT} 2025, Barcelona, Spain, March 25-28, 2025},
  pages        = {411--424},
  publisher    = {OpenProceedings.org},
  year         = {2025},
  url          = {https://doi.org/10.48786/edbt.2025.33},
  doi          = {10.48786/EDBT.2025.33}
}
```

## Install dependencies and project

```bash
pip install flwr torch transformers datasets confluent-kafka
```


## Run

### Start Mediator listening to Kafka for Parameters

Continuously listen and wait for parameters in Kafka and launch FL jobs:

```bash
python mediator.py --cleanup
```

For example, go to `/hyperparameters` and run the job:
```bash
jq -c . test_1.json | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic  FedL
```
Then, run another job (the mediator continues to listen):
```bash
jq -c . test_2.json | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic  FedL
```
And so on...

### Start Mediator Locally (for testing purposes)

Run 1-time local parameters and launch 1 job:
```bash
python mediator.py --cleanup --local /home/mtheologitis/FDA-Opt-Sys/hyperparameters/test_1.json
```

### Evaluate

```bash
python evaluate_model.py \
  --model_checkpoint prajjwal1/bert-tiny \
  --local_weights logs/checkpoints/prajjwal1-bert-tiny-039f3834.pth \
  --ds_path glue \
  --ds_name mrpc \
  --device cuda:1 \
  --num_labels 2
```
