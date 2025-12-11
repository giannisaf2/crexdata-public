FROM python:3.10

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /ui-app
COPY . /ui-app

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y vim 
    
RUN apt-get clean && \ 
    rm -rf /var/lib/apt/lists/*  

RUN pip install --upgrade pip && \ 
    pip install --no-cache-dir -r requirements.txt


