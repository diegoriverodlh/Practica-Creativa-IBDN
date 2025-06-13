# Real-Time Flight Delay Predictor

En este proyecto hemos implementado un sistema completo de analítica predictiva en tiempo real para predecir retrasos de vuelos usando diversas tecnologías y una interfaz web Flask.

## Tecnologías utilizadas

- **Apache Kafka** – Sistema de mensajería
- **Apache Spark** – Entrenamiento de modelos y predicciones en tiempo real.
- **MongoDB** – Almacenamiento de resultados.
- **Flask** – Aplicación web para introducir datos y mostrar los resultados.
- **HDFS** – Almacenamiento distribuido.
- **Apache NiFi** – Flujo de datos.


# 1. Creación de contenedores Docker

Para facilitar el arranque del proyecto, hemos utilizado el script ```empezar.sh```, que automatiza los siguientes pasos:
- Compila y empaqueta el código Sacala usando **SBT**.
- Levanta y ejecuta los contenedores definidos en el archivo ```docker-compose.yaml```
- Finalmente, muestra el estado de los contenedores Docker levantados.


# 2. Comprobamos que todo funcione correctamente

Ahora pasamos a comprobar que todas las componentes estén funcionando:

### Interfaz de Flask

Una vez desplegados los servicios con Docker Compose, la interfaz de usuario desarrollada con Flask queda disponible en el puerto 5001. Por tanto, accedemos a ella abriendo en el navegador la dirección ```http://localhost:5001/flights/delays/predict_kafka```

![Flask](https://github.com/user-attachments/assets/99c9ded3-e2f2-4541-8bcb-1ed6291e0801)

Vemos que ahora podemos introducir los datos de un vuelo y solicitar predicciones.

### MongoDB

Para facilitar la visualización de los datos almacenados, hemos incluido **Mongo-Express**, una interfaz web accesible en ```http://localhost:8093```, que nos permite navegar por la base de datos de ```agile_data_science``` y las predicciones guardadas en ella ```flight_delay_ml_response```. Desde allí podremos ver diferentes documentos, cada uno correspondiente a diferentes solicitudes que hayamos hecho desde la interfaz de Flask.

![Mongo-express](https://github.com/user-attachments/assets/cccf04f4-6255-424c-aa19-1308af19719b)

### Kafka

Para verificar que Kafka está operativo, accedemos directamente a su contenedor, nos metemos en el directorio ```/opt/bitnami/kafka/bin``` y ejecutamos el siguiente comando:
```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list 
```

Esto nos permite confirmar que se han creado correctamente los tópicos:
- ```flight-delay-ml-request```, encargado de recibir las solicitudes
- ```flight-delay-classification-response```, donde se publican las predicciones realizadas por Spark.

Ahora, para comprobar que las predicciones se están escribiendo correctamente en tiempo real, usamos un consumidor Kafka con el siguiente comando:

```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic flight-delay-classification-response \
  --from-beginning
```

![Kafka](https://github.com/user-attachments/assets/91058d76-89ae-49d0-a8f2-8204cc7a12a4)

Vemos que este comando nos muestra por consola las predicciones publicadas en el tópico.

### Spark

Para verificar el correcto funcionamiento de Spark, accedemos a la interfaz web disponible en ```http://localhost:9080```. 

![Spark](https://github.com/user-attachments/assets/60482721-93b7-49e6-9464-9a26f8c399e9)


En esta interfaz podemos confirmar que:
- El **Spark Master** está activo (```Status: ALIVE```) y corriendo.
- Hay dos nodos **Workers** listos para ejecutar tareas distribuidas.
- Vemos también la aplicación de predicción de vuelos en ejecución.


### Nifi

Accedemos a la interfaz de NiFi a través de la dirección ```http://localhost:8443/nifi``` y configuramos el siguiente flujo de trabajo que conecta dos componentes:
- **ConsumerKafka_2_0:** Un procesador que escucha el tópico ```flight-delay-classification-response``` de Kafka. Es decir, extrae los mensajes generados por Spark con las predicciones.
- **PutFile:** Este segundo procesador almacena los mensajes consumidos.

![Nifi](https://github.com/user-attachments/assets/d2b1d125-f734-45dd-9a8d-42eb578b95c2)


### HDFS

Finalmente accedemos a la interfaz de HDFS desde ```localhost://9870``` y navegamos hasta ```/user/root/flight_delay_ml_response```. Nos debería aparecer algo como esto:

![HDFS](https://github.com/user-attachments/assets/145246b2-f697-424c-9f22-3a0d2fa3c19d)

Podemos ver que las predicciones se almacenan en el formato ```Parquet```.

La alternativa para ver estos archivos por consola sería utilizar el siguiente comando desde la terminal:

```bash
hdfs dfs -ls
```
