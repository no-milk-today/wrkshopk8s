#!/bin/bash
set -e

# Загрузка переменных из .env
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Проверка переменной
if [ -z "$DOCKER_REGISTRY" ]; then
  echo "DOCKER_REGISTRY не задан в .env"
  exit 1
fi

echo "Using DOCKER_REGISTRY: $DOCKER_REGISTRY"

echo "Uninstalling Helm releases..."
for ns in test prod; do
  helm uninstall keycloak -n "$ns" || true
  helm uninstall front-ui-service -n "$ns" || true
  helm uninstall fraud-service -n "$ns" || true
  helm uninstall notification-service -n "$ns" || true
  helm uninstall exchange-generator-service -n "$ns" || true
  helm uninstall exchange-service -n "$ns" || true
  helm uninstall customer-service -n "$ns" || true
  helm uninstall apigw-service -n "$ns" || true
  helm uninstall postgres -n "$ns" || true
done

echo "Deleting secrets..."
for ns in test prod; do
  kubectl delete secret customer-service-customer-db -n "$ns" --ignore-not-found
  kubectl delete secret fraud-service-fraud-db -n "$ns" --ignore-not-found
  kubectl delete secret notification-service-notification-db -n "$ns" --ignore-not-found
  kubectl delete secret exchange-service-exchange-db -n "$ns" --ignore-not-found
done

echo "Deleting namespaces..."
kubectl delete ns test --ignore-not-found
kubectl delete ns prod --ignore-not-found

echo "Shutting down Jenkins..."
docker compose down -v || true
docker stop jenkins && docker rm jenkins || true
docker volume rm jenkins_home || true

echo "Removing images..."
docker image rm customer-service:latest || true
docker image rm fraud-service:latest || true
docker image rm notification-service:latest || true
docker image rm exchange-service:latest || true
docker image rm exchange-generator-service:latest || true
docker image rm front-ui-service:latest || true
docker image rm apigw-service:latest || true
docker image rm jenkins-jenkins:latest || true

echo "Pruning system..."
#docker system prune -af --volumes

echo "Done! All clean."
