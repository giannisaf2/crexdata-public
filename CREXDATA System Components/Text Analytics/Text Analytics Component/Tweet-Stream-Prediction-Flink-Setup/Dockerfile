FROM flink:scala_2.12-java11

RUN apt-get update && \
    apt-get install -y \
    x11-apps \
    wget \
    libgtk-3-0 \
    libasound2 && \
    apt-get clean

RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    ln -s /usr/bin/python3 /usr/bin/python

RUN pip3 install apache-flink==2.0.0 ftfy==6.3.1

COPY shared/relevance_job_utils/pyflink.zip /opt/flink/opt/python/pyflink.zip
COPY shared/relevance_job_utils/kafka-clients-4.0.0.jar /opt/flink/lib/kafka-clients-4.0.0.jar