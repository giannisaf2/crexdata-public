FROM python:3.10

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /crawler-app
COPY . /crawler-app

RUN apt-get update && \
    apt-get -y upgrade && \
    apt-get install -y vim 

RUN yes | wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb &&\
    (dpkg -i google-chrome-stable_current_amd64.deb || \
    (apt-get -f install -y && dpkg -i google-chrome-stable_current_amd64.deb)) 
    
RUN apt-get clean && \ 
    rm -rf /var/lib/apt/lists/* && \
    rm google-chrome-stable_current_amd64.deb 

RUN pip install --upgrade pip && \ 
    pip install --no-cache-dir -r requirements.txt


