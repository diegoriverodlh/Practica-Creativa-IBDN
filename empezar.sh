#!/bin/bash

set -e  # Detiene el script si algún comando falla

PROYECTO_DIR="/home/diego.rivero/practica_creativa/flight_prediction"

echo "Cambiando al directorio del proyecto: $PROYECTO_DIR"
cd "$PROYECTO_DIR"

echo "Compilando el proyecto con sbt..."
sbt compile > /dev/null 2>&1

echo "Empaquetando el proyecto con sbt..."
sbt package > /dev/null 2>&1

echo "Volviendo al directorio original..."
cd - > /dev/null


DOCKER_COMPOSE_FILE="/home/diego.rivero/docker-compose.yaml"
echo "Deteniendo contenedores Docker..."
docker compose -f "$DOCKER_COMPOSE_FILE" down > /dev/null 2>&1

echo "Levantando contenedores Docker con build..."
docker compose -f "$DOCKER_COMPOSE_FILE" up -d --build > /dev/null 2>&1

echo "Visualizando contenedores..."
docker ps -a

echo "Todo completado correctamente."
