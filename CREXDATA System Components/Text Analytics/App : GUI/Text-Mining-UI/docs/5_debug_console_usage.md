# [Debug and Monitor Kafka Latency in Text Mining UI](https://app.tango.us/app/workflow/22657638-50e9-4583-b862-5007e2854202?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)

Step-by-step guide using the debug/log tab.

***




### 1. Navigate to the link for the text mining app ex:http://localhost:7860/


### 2. Follow steps in setup kafka cluster documentation.


### 3. Check "Show latency plot" to monitor the latency.
![Step 3 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/12583233-fa96-444c-9384-671415c78c63/275b147d-8a8e-408c-a2f4-bdea03b48ec0.png?crop=focalpoint&fit=crop&fp-x=0.0183&fp-y=0.7027&fp-z=3.2154&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=50&mark-y=338&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz00MiZoPTQyJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 4. Then click on "Connect to kafka".
![Step 4 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/305987cd-e8d1-4c15-a4d8-1f1240cbf675/7ce2a577-0ca7-4b6a-9f8a-12ddb1f13756.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.7372&fp-z=2.3444&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=21&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zNTYmaD00NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 5. Navigate to the "DEBUG/LOG Console".
![Step 5 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/1297d16b-cc9e-446a-b985-f9a2564299e1/d5def2af-a18f-4c28-9d2c-4fe4e90b71fa.png?crop=focalpoint&fit=crop&fp-x=0.2199&fp-y=0.0215&fp-z=2.7016&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=486&mark-y=12&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0yMjcmaD01OSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 6. Here you can see the latency plot, the "green" line is overall system latency, "red" is flink streaming lag, and "blue" is the relevance model inference time. All in (secs).
![Step 6 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/29d77bb3-eafb-4f92-9b10-15eae2a79ec2/ca5ccd91-0550-427e-b48f-6e2dd4edeeb3.png?crop=focalpoint&fit=crop&fp-x=0.4966&fp-y=0.2513&fp-z=1.0187&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=15&mark-y=55&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTY5Jmg9MjU3JmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 7. You can also get statistics about a kafka topic. Enter the topic name in the provided field.
![Step 7 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/e44e6fcc-792e-407d-bf83-914e7a178f72/718fa7be-3769-4f16-a4a5-dec17d355574.png?crop=focalpoint&fit=crop&fp-x=0.4966&fp-y=0.4989&fp-z=1.0187&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=15&mark-y=347&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTY5Jmg9MjUmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 8. Then click "Refresh"
![Step 8 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/56d32b92-88d6-468f-a9cb-d0ea485962ac/c063a133-d074-48b6-8037-33986a2ce898.png?crop=focalpoint&fit=crop&fp-x=0.8164&fp-y=0.5437&fp-z=2.8713&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=53&mark-y=309&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMDk1Jmg9MTAwJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 9. You can view the number of messages in the topic.
![Step 9 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/a67021e7-914e-42f5-a7e2-0446e8e6953c/085e49d4-1530-4934-a56b-b750e6699fe8.png?crop=focalpoint&fit=crop&fp-x=0.1768&fp-y=0.5521&fp-z=1.6212&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=36&mark-y=342&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz02MTYmaD0zNSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 10. You can also view the timestamp of the last message received by the topic.
![Step 10 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/246c35ef-d2be-4225-a731-3915e930c5ba/88120ca4-1da2-4893-b3ec-50d652ed789f.png?crop=focalpoint&fit=crop&fp-x=0.4963&fp-y=0.5521&fp-z=1.6212&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=292&mark-y=342&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz02MTYmaD0zNSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 11. Then below you can see the contents of the last message received by the topic. 
![Step 11 screenshot](https://images.tango.us/workflows/22657638-50e9-4583-b862-5007e2854202/steps/d2a6c00c-6ec1-4639-b3db-971ce49242bd/b51290d0-5dff-4a09-8395-66a0f5c9c058.png?crop=focalpoint&fit=crop&fp-x=0.5094&fp-y=0.6046&fp-z=1.0501&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=18&mark-y=412&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTY0Jmg9MTYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 12. You can click "Refresh" as much as you want see the progress of the messages received by the topic. This is useful when checking the status of post crawled, or posts that have been processed by the event type classification model (relevance prediction).

<br/>

***
Created with [Tango.ai](https://tango.ai?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)