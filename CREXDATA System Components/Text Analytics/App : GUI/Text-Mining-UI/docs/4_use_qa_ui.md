# [How to use question answering UI](https://app.tango.us/app/workflow/fd036150-c28a-42ff-a597-d079743dbebf?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)

This is a step-by-step guide on how to use question answering in the text mining UI.

***




### 1. Navigate to the link for the text mining app ex:http://localhost:7860/


### 2. Follow steps in setup kafka cluster documentation, then click "Connect to kafka".
![Step 2 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/52e6940a-816f-4c11-87a9-71241ae16b3c/e22c21de-9dde-4e05-9e88-c093c0371b7b.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.7372&fp-z=2.3444&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=21&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zNTYmaD00NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 3. Navigate to the "Crawler UI" tab.
![Step 3 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/48cd2e82-f0eb-44c8-8ab3-f7a8adb6c67d/87441fa2-9a1e-4184-a985-04291ce53bfb.png?crop=focalpoint&fit=crop&fp-x=0.1373&fp-y=0.0215&fp-z=2.6685&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=320&mark-y=12&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0yMzkmaD01OSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 4. In the "Kafka poling parameters" you can set a filer for the ID of the event, used to crawl it.
![Step 4 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/bc53f7fc-54ec-4ed2-a16b-76d934e7112d/a42b1777-91e7-40e9-8064-ba5044bf6ed1.png?crop=focalpoint&fit=crop&fp-x=0.2625&fp-y=0.1326&fp-z=1.3019&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=44&mark-y=108&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz03MzEmaD0zMiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 5. You can also set the number of messages to retrieve.
![Step 5 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/0cec075f-a59d-4e93-86c6-4e5dbc301a74/fd428fd0-55b6-4eba-b529-29c0459cea80.png?crop=focalpoint&fit=crop&fp-x=0.7375&fp-y=0.1333&fp-z=2.0139&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=34&mark-y=167&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTMxJmg9NTEmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 6. Then click on "Get recent posts" to retrieve messages.
![Step 6 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/e03ada43-f4ff-40c6-a2c5-68c4ba989a8a/ec03e751-aedf-4e44-b0eb-83859ea3e089.png?crop=focalpoint&fit=crop&fp-x=0.5000&fp-y=0.1762&fp-z=1.0187&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=11&mark-y=117&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTc4Jmg9MjUmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 7. The messages are displayed in a table.
![Step 7 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/62981317-8734-4f87-b922-f6f4d74e0f6d/3edbc11e-54b4-45eb-84c8-9d623c29e90d.png?crop=focalpoint&fit=crop&fp-x=0.6414&fp-y=0.6713&fp-z=1.4444&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=21&mark-y=81&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTU3Jmg9NTkzJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 8. Once relevant messages are retrieved, you can set the question answering parameters. 
![Step 8 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/9f80d9bc-cbdd-4c68-8e9a-62576b748774/2cb014fc-af95-4aa5-abbe-e4916a0ca3a3.png?crop=focalpoint&fit=crop&fp-x=0.2584&fp-y=0.2517&fp-z=1.3160&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=45&mark-y=229&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz03MjYmaD0xOCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 9. In "Choose QA type" you can set the method for QA. 
1. no-RAG, doesn't use retrieval augmented generation and is fast.
2. Genra RAG, is our method that uses retrieval augmented generation.
![Step 9 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/801ba430-d254-483f-8b90-235ef826ec98/12fd92ca-de68-48f3-aedc-f182f8316d8e.png?crop=focalpoint&fit=crop&fp-x=0.2588&fp-y=0.2759&fp-z=1.2988&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=242&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz03MzImaD0zMCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 10. You can also choose the language the LLM responds in, with English, German, Spanish, and Catalan options.
![Step 10 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/ce779ed2-7e4e-4c43-8b1d-83f798f10773/232cb49f-ab49-40ad-8f1f-9c128fa64233.png?crop=focalpoint&fit=crop&fp-x=0.7357&fp-y=0.2759&fp-z=2.0028&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=35&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTI5Jmg9NDYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 11. You can also select the type of query. 
"query" - allows you to ask a custom question
"summarise" - gives a summary of the crisis incident described in the messages
![Step 11 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/62615e28-885a-46bd-8df0-adf2f2b1aa41/2d1cbba0-0106-43a7-b99a-b696b5f590b8.png?crop=focalpoint&fit=crop&fp-x=0.2588&fp-y=0.3456&fp-z=1.2988&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=37&mark-y=307&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz03MzImaD0zMCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 12. You can also select predefined options when the "query" option is selected.
![Step 12 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/08281ad9-00ea-4dcc-9c74-9aeeaf937c5f/6091d563-759a-44f4-a19b-879c6e8193c7.png?crop=focalpoint&fit=crop&fp-x=0.1034&fp-y=0.4490&fp-z=2.1230&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=46&mark-y=338&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz00MzYmaD00MiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 13. Then click on "Run QA".
![Step 13 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/928379a7-6d4a-427d-97ef-47223f3f98a3/ae658767-54f4-4a7a-a6fb-f2a4a5ea246b.png?crop=focalpoint&fit=crop&fp-x=0.4966&fp-y=0.5096&fp-z=1.0182&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=15&mark-y=347&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTcwJmg9MjUmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 14. The LLM response is displayed in the text field.
![Step 14 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/e6c26d9b-5150-4be0-9e99-0ff39e7ce1a6/d694de8f-4549-4e6d-bd85-1f2eb75c3477.png?crop=focalpoint&fit=crop&fp-x=0.4966&fp-y=0.8341&fp-z=1.0182&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=15&mark-y=589&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTcwJmg9MTYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 15. You can also click on "tweet_id" in the table to view the embedded post from X.
![Step 15 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/0001d7c7-6231-41cd-9fbe-c67ad5eceb09/d67687ea-bd2d-42c5-baf0-fdca141e802c.png?crop=focalpoint&fit=crop&fp-x=0.1619&fp-y=0.6015&fp-z=1.6353&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=35&mark-y=109&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz01NjUmaD01MDAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 16. Click on Kafka polling parameters…
![Step 16 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/04f93e9c-1971-43cd-84eb-791fb24d16d9/1bdabc67-98c8-4146-90bb-49294829f353.png?crop=focalpoint&fit=crop&fp-x=0.4966&fp-y=0.5000&fp-z=1.0031&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=15&mark-y=1&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTY1Jmg9NzE2JmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 17. Also note, when the "summarise" option is selected in the "Choose Query Type", the query textbox is not visible.
![Step 17 screenshot](https://images.tango.us/workflows/fd036150-c28a-42ff-a597-d079743dbebf/steps/f10353b1-cca9-4554-8a65-4c46a7b93f6a/f300187a-a671-47fa-9308-17097b43e71b.png?crop=focalpoint&fit=crop&fp-x=0.4966&fp-y=0.5000&fp-z=1.0031&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=15&mark-y=1&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTY1Jmg9NzE2JmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)

<br/>

***
Created with [Tango.ai](https://tango.ai?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)