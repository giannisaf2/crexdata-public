# [Setup Kafka clusters in UI](https://app.tango.us/app/workflow/f49f9572-8176-4df6-a700-361fda753896?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)

This is a guide to setting up the Kafka clusters using input provided in the text mining UI sidebar.

***




### 1. Navigate to the link for the text mining app ex:http://localhost:7860/


### 2. Click on "Choose server" in sidebar
![Step 2 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/b1204e02-54d7-4818-a1f4-0799a1f8c91b/863cc097-189b-4b50-a87b-211eecc61361.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.2218&fp-z=2.4664&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=53&mark-y=342&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMTImaD0zNCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 3. Set to "crexdata-auth" if using authenticated server.

Note: The authentication settings should have been previously configured. See
README.md
![Step 3 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/5cde172c-37df-4f73-99b8-3c7822f5942b/aaa20126-e334-4175-ad7c-a1f8dda50c15.png?crop=focalpoint&fit=crop&fp-x=0.0711&fp-y=0.2460&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=38&mark-y=331&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 4. Select "crexdata-unauth" if using an unauthenticated server.
![Step 4 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/60f8c174-6305-425b-903f-e54adfa1b82d/2d0e2595-df89-4a39-b2a0-2635e51fa2cc.png?crop=focalpoint&fit=crop&fp-x=0.0711&fp-y=0.2720&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=38&mark-y=331&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 5. The "Bootstrap server" field becomes visible if using an unauthenticated server.

Fill in the bootstrap server IP and port, ex: localhost:9092
![Step 5 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/cab6e74c-3254-401a-86a9-8e2189da3629/1ab8814d-d29d-4fbf-a91f-49855c663876.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.2797&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=330&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01OCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 6. In the "Kafka topic with relevant tweets" field, you can set the Kafka topic used in the "Relevance UI" tab to get posts relevant to a weather crisis.

Note: This topic needs to already have posts classified as "fire" or "flood" using the event type prediction model.
![Step 6 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/a6e8ccec-bc28-46ab-a9eb-cc3a0a804ca7/072e0056-5218-410d-82d3-6ee129e8307d.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.3464&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=318&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD04MiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 7. In the "Kafka topic sending to ARGOS" field, you can set the Kafka topic to send
posts to ARGOS.

Note: This topic should be monitored by the ARGOS app
![Step 7 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/a5e48775-23ac-4b75-ab4d-f6f16eda90a3/89775b03-84e9-426d-8375-99948869e59d.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.4130&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=330&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01OCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 8. In the "Kafka topic with relevant tweets for QA" field, you can set the topic used in the "Question Answering UI" tab to get posts relevant to a weather crisis for question answering.
![Step 8 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/91bc0797-45c4-4994-93d7-1767e4bb05e5/e56a1954-2fc4-40ea-8b0b-07c5f29af524.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.4805&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=318&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD04MiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 9. In the "Kafka topic crawler commands" field, you can set the topic to send crawler commands.

Note: This topic should be configured in crawler component. (see X_Crawler)
![Step 9 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/6e8a7d21-e1ff-40b4-85ed-89ff75606717/a7b6b271-1402-4016-9829-1767e4c45456.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.5471&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=330&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01OCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 10. In the "Kafka topic with crawler logs" field, you can set the topic to get crawler
logs.

Note: This topic should be configured in crawler component. (see X_Crawler)
![Step 10 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/4d884d03-441a-49a2-9b37-fb0b1deb9389/714eb0dd-350e-4843-8ce5-445655ed76f2.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.6069&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=330&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01OCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 11. In the "kafka topic with processed tweets for latency" field, you can set the topic with post processed by the event type prediction component. These posts have latency calculation and allow viewing a latency graph, for debug purposes.
![Step 11 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/85dd1079-2efa-4ecd-9f8d-a8c311dfdace/fe4b80b8-f44b-4bc2-b04e-dddd61e75cf0.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.6674&fp-z=2.4065&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=330&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zMzQmaD01OCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 12. Uncheck "Show latency plot" field if you don't want the latency graph on the
"DEBUG/LOG Console" tab.

Check "Show latency plot" field if you don't want the latency graph on the
"DEBUG/LOG Console" tab.
![Step 12 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/0960430b-b841-4ae5-94ab-584257d0b73d/238bd804-f15b-4a4f-abaf-0089bcf84987.png?crop=focalpoint&fit=crop&fp-x=0.0183&fp-y=0.7027&fp-z=3.2154&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=50&mark-y=338&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz00MiZoPTQyJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 13. Click "Connect to kafka" to finalize the config process and connect to kafka
clusters.
![Step 13 screenshot](https://images.tango.us/workflows/f49f9572-8176-4df6-a700-361fda753896/steps/b2f36431-4b25-4095-a9df-19670c80d6cc/4c20ee1b-e081-4912-9ea1-56c1af572598.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.7372&fp-z=2.3444&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=21&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zNTYmaD00NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)

<br/>

***
Created with [Tango.ai](https://tango.ai?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)