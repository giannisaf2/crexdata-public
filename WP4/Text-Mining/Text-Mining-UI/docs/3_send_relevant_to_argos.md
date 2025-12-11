# [How to Save Relevant Posts to ARGOS](https://app.tango.us/app/workflow/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)

This guide provides a step-by-step process for viewing and saving relevant posts to ARGOS using the Text Mining UI app.

***




### 1. Navigate to the link for the text mining app ex:http://localhost:7860/


### 2. Follow steps in setup kafka cluster documentation, then click "Connect to kafka".
![Step 2 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/083225b6-09ed-4ced-87d5-4e301fd3db3f/cb4ffe96-321f-49b0-a720-af051209b17f.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.7372&fp-z=2.3444&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=21&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zNTYmaD00NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 3. You can click on "Toggle Sidebar arrow" to minimize to sidebar.
![Step 3 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/2cefd23f-a632-4a35-869c-aee2b49eeaba/b91f485e-2db3-4dc2-bd86-a17dd8ffb2da.png?crop=focalpoint&fit=crop&fp-x=0.1403&fp-y=0.0245&fp-z=3.0243&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=479&mark-y=20&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz02MCZoPTY3JmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 4. Navigate to the "Revelance UI" tab.
![Step 4 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/4f221f74-ba1a-4674-b905-691bc7b74e1c/88c4fce8-cc57-4feb-95a6-b9f5a2c7585f.png?crop=focalpoint&fit=crop&fp-x=0.0784&fp-y=0.0215&fp-z=2.8536&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=182&mark-y=13&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xNzMmaD02MyZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 5. On this tab, you expand the "kafka poling parameters" to configure how many
messages are retrieved from the cluster.
![Step 5 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/3f85c69d-6245-49db-baa3-44f783751ed4/eda0f939-44d7-4535-adc3-52b6c98f51a9.png?crop=focalpoint&fit=crop&fp-x=0.5000&fp-y=0.0674&fp-z=1.0239&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=14&mark-y=42&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTcxJmg9MTUmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 6. Click the "Number of messages to return" field, and set the number or leave it at 10.
![Step 6 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/58c9e803-12a7-405d-86e9-133c8ae91757/afca23bf-da85-432d-bb6d-3567888fe35e.png?crop=focalpoint&fit=crop&fp-x=0.2625&fp-y=0.1333&fp-z=1.3019&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=44&mark-y=108&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz03MzEmaD0zMyZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 7. You can use the "Poll Timeout" slider to set the number secs to wait when
retrieving messages from kafka. You can also leave the value as default (6secs).
![Step 7 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/7b338eca-264a-48ce-95da-38db9b85f0ac/ea81521a-7c9b-4936-ba9a-0bccd9a0050b.png?crop=focalpoint&fit=crop&fp-x=0.7357&fp-y=0.1310&fp-z=2.0421&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=48&mark-y=183&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTA0Jmg9MTgmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 8. Then click "Get relevant posts" to get posts.
![Step 8 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/d6bae321-bac7-4c81-936a-3fc65e3da599/56e3a76c-2603-433a-97ae-d9f20aaefb33.png?crop=focalpoint&fit=crop&fp-x=0.5000&fp-y=0.1732&fp-z=1.0187&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=11&mark-y=117&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTc4Jmg9MjAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 9. You can select a tweet_id" in the table to view Embedded tweet.
![Step 9 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/d02c881e-c2be-417e-8f4e-f1ce86a37a16/1d16cb33-f77b-4d9b-8330-d9f7a07c00d9.png?crop=focalpoint&fit=crop&fp-x=0.5000&fp-y=0.2678&fp-z=1.0182&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=11&mark-y=188&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTc4Jmg9MTYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 10. Scroll through the retrieved messages and click on the "tweet_id" column to view the embedded post.
![Step 10 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/e2f0861d-27da-4d45-a871-1fca0cf89acf/b56a60d9-4426-44f2-a99a-6199749a3c9c.png?crop=focalpoint&fit=crop&fp-x=0.1630&fp-y=0.4943&fp-z=1.3913&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=30&mark-y=146&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz00ODUmaD00MjYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 11. You can also click on the embedding to view on X.
![Step 11 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/d3f6d8a2-4908-4fb3-b6d9-107d8ebc4aae/f0973f62-7f42-486d-bac3-7bc96d82727f.png?crop=focalpoint&fit=crop&fp-x=0.1630&fp-y=0.4943&fp-z=1.3913&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=30&mark-y=146&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz00ODUmaD00MjYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 12. Select the most relevant posts to send to ARGOS, matching the numbers to the
indices in the table.
![Step 12 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/343a243f-df6d-4e42-ae96-c3661a98c71e/6d9a541d-062d-47a4-b035-0cdf89400a57.png?crop=focalpoint&fit=crop&fp-x=0.4301&fp-y=0.2314&fp-z=3.1408&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=579&mark-y=338&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz00MSZoPTQxJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 13. Then click "Save to ARGOS" to send posts.
![Step 13 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/9d1b2d5c-b1b6-4cd9-b2f5-923b647a6386/9867a0c0-0c0a-470c-8842-38df3771346b.png?crop=focalpoint&fit=crop&fp-x=0.8221&fp-y=0.2222&fp-z=2.7561&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=82&mark-y=297&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMDU4Jmg9MTI0JmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 14. An info will display showing the messages being sent.
![Step 14 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/cd66d8ad-d2b2-4f67-94e6-53e9909cf737/41675d5d-5854-4c61-bd54-a7be4ba4491e.png?crop=focalpoint&fit=crop&fp-x=0.8939&fp-y=0.1103&fp-z=3.0815&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=535&mark-y=217&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz01NDQmaD01NCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 15. Then you can click "Get relevant posts", to get a new batch of posts, and repeat the same process again.
![Step 15 screenshot](https://images.tango.us/workflows/f5f82f69-ec30-4f8c-b3ed-0c0b2d4829b7/steps/c27e3363-f32c-472f-bb75-5d7e7d239e65/b5debcd0-4557-47ab-b581-30748c0fa93c.png?crop=focalpoint&fit=crop&fp-x=0.5000&fp-y=0.1732&fp-z=1.0187&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=11&mark-y=117&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTc4Jmg9MjAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)

<br/>

***
Created with [Tango.ai](https://tango.ai?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)