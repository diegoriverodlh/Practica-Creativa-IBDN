FROM python:3.8-slim

ENV AIRFLOW_HOME=/opt/airflow

# Copiamos todos los recursos de la práctica
COPY practica_creativa ./practica_creativa

# Instalamos dependencias de Airflow
RUN pip install -r practica_creativa/resources/airflow/requirements.txt \
    -c practica_creativa/resources/airflow/constraints.txt

# Instalamos otros requisitos generales
RUN pip install -r practica_creativa/requirements.txt

# Creamos las carpetas necesarias
RUN mkdir -p $AIRFLOW_HOME/dags $AIRFLOW_HOME/logs $AIRFLOW_HOME/plugins

# Copiamos el DAG de configuración inicial
RUN cp practica_creativa/resources/airflow/setup.py $AIRFLOW_HOME/dags/

COPY dockerfiles/airflow/supervisord.conf /etc/supervisord.conf


# Instalamos Java y Spark
RUN apt-get update && apt-get install -y wget gnupg && \
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-11-jdk procps supervisor curl


ENV SPARK_VERSION=3.5.1
ENV HADOOP_VERSION=3
ENV SPARK_HOME=/opt/spark
ENV PATH="${SPARK_HOME}/bin:${PATH}"

RUN curl -L https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz \
    | tar -xz -C /opt && \
    mv /opt/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} ${SPARK_HOME}

EXPOSE 8180

# Inicializa la DB y lanza Airflow con supervisord
CMD bash -c "airflow db init && \
airflow users create --username admin --firstname Jose --lastname Pablo --role Admin --email example@email.org --password admin && \
supervisord -c /home/diego.rivero/practica_creativa/resources/airflow/supervisord.conf"

