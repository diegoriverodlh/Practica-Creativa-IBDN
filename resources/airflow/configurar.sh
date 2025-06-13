#!/bin/bash

# Activar entorno virtual
source ~/practica_creativa/env/bin/activate

# Definir ruta personalizada para AIRFLOW_HOME
export AIRFLOW_HOME=~/practica_creativa/resources/airflow
echo "Usando AIRFLOW_HOME en: $AIRFLOW_HOME"

# Crear carpetas necesarias (no da error si ya existen)
mkdir -p $AIRFLOW_HOME/dags
mkdir -p $AIRFLOW_HOME/logs
mkdir -p $AIRFLOW_HOME/plugins

# Dar permisos por si acaso
chmod -R 777 $AIRFLOW_HOME

# Inicializar la base de datos (usa migrate en vez de init si lo prefieres)
airflow db init

# Crear usuario admin
airflow users create \
    --username admin \
    --firstname Jack \
    --lastname Sparrow \
    --role Admin \
    --email example@mail.org \
    --password admin

echo "✅ Configuración de Airflow completada."
