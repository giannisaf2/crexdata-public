# [How to Start and Stop a Crawl in the text mining UI](https://app.tango.us/app/workflow/0551a34c-ae41-44ab-9c80-ccd11fbe54b5?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)

This guide provides a step-by-step process to start and stop a crawl using the text mining UI.

***




### 1. Navigate to the link for the text mining app ex:http://localhost:7860/


### 2. Follow steps in setup kafka cluster documentation, then click "Connect to kafka".
![Step 2 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/62b13745-8b57-4162-a5cb-7c7a852b3111/fdc464cc-95ff-4251-99ee-c3ef77fdce1a.png?crop=focalpoint&fit=crop&fp-x=0.0706&fp-y=0.7372&fp-z=2.3444&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=21&mark-y=336&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0zNTYmaD00NiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 3. Navigate to the "Crawler UI" tab. 
![Step 3 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/d9a4ac1a-9cea-494d-8874-4b6d76e285a3/0690f2bd-6018-445d-b443-65831bcb4402.png?crop=focalpoint&fit=crop&fp-x=0.0348&fp-y=0.0215&fp-z=2.9068&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=45&mark-y=13&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xNTQmaD02NCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 4. Click the "Crawl Identifier" field, and type in a unique identify to manage the crawl. ex: flood_<PLACE>_<DATE>.
![Step 4 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/fd9f86ac-7dd0-4715-9321-d799e504ba8d/2aff9cab-d86d-49f0-9dc1-73ea54f0c635.png?crop=focalpoint&fit=crop&fp-x=0.1800&fp-y=0.1310&fp-z=1.6309&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=46&mark-y=134&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz02MTMmaD0zOSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 5. Fill in the other details, include the X login details, language of posts, lon, lat, and radius of area to crawl from, then click "Start Crawl".
![Step 5 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/20d3ab66-a9ce-44ae-a05c-af13a318eaea/e34354a9-66ed-42c2-817e-adf63e296892.png?crop=focalpoint&fit=crop&fp-x=0.5000&fp-y=0.3157&fp-z=1.0187&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=11&mark-y=221&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0xMTc4Jmg9MjAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


ℹ️ Note: We advise using a Gmail account to create an X account for crawling, andalso setting up an app passwords. With these steps:1. create Gmail account2. visit google security settings to turn on 2fa - https://myaccount.google.com/security3. add mobile number for 2fa4. visit https://myaccount.google.com/apppasswords5. create new app to access account6. copy app password


### 6. Click on Info…A notification will show that the crawl command has been sent.
![Step 6 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/8d10d6b8-e2a4-4cdd-90dc-4054b8f9bd0d/566e23d2-75df-406b-bb2e-b6a2d8aa4ccc.png?crop=focalpoint&fit=crop&fp-x=0.8939&fp-y=0.0307&fp-z=3.0961&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=532&mark-y=43&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz01NDcmaD01MSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 7. Now you can navigate to "DEBUG/LOG Console" tab to see the crawler logs and
commands.
![Step 7 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/2f7efa1a-4eb1-49ec-b30a-c0ee4dbc97ca/49423ecf-f40e-423a-934c-eaafe01a21ed.png?crop=focalpoint&fit=crop&fp-x=0.2061&fp-y=0.0215&fp-z=2.7016&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=486&mark-y=12&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0yMjcmaD01OSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 8. In the Crawler log console, there are two consoles, the left is for crawler
commands.

You can set length of latest commands returned using the log tail field.
![Step 8 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/3e4f4147-c0ce-48b6-a95d-7ec7ae290caf/450a0c95-727c-4076-849a-745a418b1aaa.png?crop=focalpoint&fit=crop&fp-x=0.2107&fp-y=0.3770&fp-z=1.5049&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=51&mark-y=340&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz02NTgmaD0zOCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 9. Then click "Refresh" to view logs.
![Step 9 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/690b8a25-5573-4cc6-8500-9be58822e095/d50a210b-f7f3-4413-be3f-5703c775b913.png?crop=focalpoint&fit=crop&fp-x=0.4493&fp-y=0.3582&fp-z=2.4881&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=452&mark-y=268&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0yOTYmaD0xODImZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 10. The crawl commands are show in the text console below.
![Step 10 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/d9487c1d-72c3-4094-8c48-8e04d5fe8b09/3d47fc21-aa6d-4d64-8cff-26c890d84906.png?crop=focalpoint&fit=crop&fp-x=0.2586&fp-y=0.5693&fp-z=1.2812&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=28&mark-y=206&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz03MzkmaD0zMDYmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 11. For the right console, you can see the crawler logs.
![Step 11 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/7d228f5f-6d3d-4666-b603-43e30327ecc0/cffe66ab-32bb-431b-9d1a-bc274412480c.png?crop=focalpoint&fit=crop&fp-x=0.6939&fp-y=0.3241&fp-z=2.0270&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=144&mark-y=345&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz05MTEmaD0yOSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 12. You can also set the log tail and further filter the logs by, ERROR logs, WARNING
logs, or INFO logs by clicking on the corresponding button to populate the filter
field.

Note: you can also enter the unique crawl id used to start the crawl to view logs specific to that crawl.
![Step 12 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/22011f9f-0672-41a9-b879-3fb51b7b6dbb/095a37ee-b17d-4dbc-a86e-a3fc3292ac09.png?crop=focalpoint&fit=crop&fp-x=0.6939&fp-y=0.3770&fp-z=2.0479&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=152&mark-y=333&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz04OTYmaD01MiZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 13. You can select "INFO" or any other predefined filter.
![Step 13 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/76ef44c2-3d05-4895-880a-0cc7dfdfc965/fda029ca-bfc8-4612-8629-0389210bdb52.png?crop=focalpoint&fit=crop&fp-x=0.5821&fp-y=0.4889&fp-z=3.0526&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=555&mark-y=329&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz04OSZoPTYwJmZpdD1jcm9wJmNvcm5lci1yYWRpdXM9MTA%3D)


### 14. The click "Refresh" to view the logs.
![Step 14 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/1d7c50fe-1e26-4985-8fcd-716ae087da3e/4f2a1106-0823-42a8-b195-4989906f8eba.png?crop=focalpoint&fit=crop&fp-x=0.9321&fp-y=0.4088&fp-z=1.9878&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=920&mark-y=214&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0yMzYmaD0yOTAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 15. The logs are displayed in the text console below.
![Step 15 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/8af10f3f-6f5c-4c1b-9d21-a3d16e9c6cd6/20ba368d-e0bd-4e2c-8423-6e104e2c5481.png?crop=focalpoint&fit=crop&fp-x=0.6939&fp-y=0.4383&fp-z=2.0479&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=152&mark-y=334&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz04OTYmaD01MCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 16. You can also type in the unique crawl ID used to start the crawl in this filter field, so you see only the logs related to that crawl.
![Step 16 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/786355d8-9d28-4c15-aa29-0e009d59d8cb/06cbd555-2a5f-4467-8eeb-aa192782eb8f.png?crop=focalpoint&fit=crop&fp-x=0.6939&fp-y=0.4383&fp-z=2.0479&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=152&mark-y=334&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz04OTYmaD01MCZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 17. Finally, you can keep changing the filter and clicking "Refresh" as many times as you need.
![Step 17 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/4618937f-9eeb-4451-82e7-74f08787408c/dc07704d-1d42-4201-a530-9c1049778a96.png?crop=focalpoint&fit=crop&fp-x=0.9321&fp-y=0.4088&fp-z=1.9878&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=920&mark-y=214&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz0yMzYmaD0yOTAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 18. To kill crawl, navigate back to the "Crawl UI" and enter the unique id for a running crawl in the "Kill Crawler Instance" section.
![Step 18 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/afb9aecc-2f3f-4e71-bd40-db6747bf269c/4057df22-852e-4870-a541-ac8fbb05df66.png?crop=focalpoint&fit=crop&fp-x=0.3558&fp-y=0.4138&fp-z=1.0364&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=29&mark-y=295&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz04MjcmaD0yNSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)


### 19. Then click on "Kill Crawl".
![Step 19 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/49909640-dd82-4e49-9104-30801923da24/ee010e4d-78cc-44c9-84ca-09a682fb7096.png?crop=focalpoint&fit=crop&fp-x=0.8359&fp-y=0.3981&fp-z=2.6390&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=218&mark-y=284&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz05MjMmaD0xNTAmZml0PWNyb3AmY29ybmVyLXJhZGl1cz0xMA%3D%3D)


### 20. A final notification will pop up showing the kill command has been sent.
![Step 20 screenshot](https://images.tango.us/workflows/0551a34c-ae41-44ab-9c80-ccd11fbe54b5/steps/4edb46f1-55a4-4d88-9486-053b08ea58de/aa93c058-4441-4fe1-9219-be42d75ff285.png?crop=focalpoint&fit=crop&fp-x=0.8939&fp-y=0.0307&fp-z=3.0961&w=1200&border=2%2CF4F2F7&border-radius=8%2C8%2C8%2C8&border-radius-inner=8%2C8%2C8%2C8&blend-align=bottom&blend-mode=normal&blend-x=0&blend-w=1200&blend64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL21hZGUtd2l0aC10YW5nby13YXRlcm1hcmstdjIucG5n&mark-x=532&mark-y=43&m64=aHR0cHM6Ly9pbWFnZXMudGFuZ28udXMvc3RhdGljL2JsYW5rLnBuZz9tYXNrPWNvcm5lcnMmYm9yZGVyPTQlMkNGRjc0NDImdz01NDcmaD01MSZmaXQ9Y3JvcCZjb3JuZXItcmFkaXVzPTEw)

<br/>

***
Created with [Tango.ai](https://tango.ai?utm_source=markdown&utm_medium=markdown&utm_campaign=workflow%20export%20links)