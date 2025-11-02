#!/bin/bash

# This script deploys selected microservices to a Kubernetes cluster using Helm.

# Set the image tag
IMAGE_TAG="latest"
NAMESPACE="prod"

echo "Deploying services with IMAGE_TAG=${IMAGE_TAG} to namespace ${NAMESPACE}..."

helm upgrade --install customer-service my-microservices-app/charts/customer-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=customer-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=customer.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm upgrade --install fraud-service my-microservices-app/charts/fraud-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=fraud-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=fraud.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm upgrade --install notification-service my-microservices-app/charts/notification-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=notification-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=notification.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm upgrade --install cash-service my-microservices-app/charts/cash-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=cash-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=cash.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm upgrade --install transfer-service my-microservices-app/charts/transfer-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=transfer-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=transfer.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm upgrade --install front-ui-service my-microservices-app/charts/front-ui-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=front-ui-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=front-ui.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm upgrade --install apigw-service my-microservices-app/charts/apigw-service \
  --namespace ${NAMESPACE} --create-namespace \
  --set image.repository=apigw-service \
  --set image.tag=${IMAGE_TAG} \
  --set ingress.enabled=true \
  --set ingress.hosts[0].host=apigw.prod.local \
  --set ingress.hosts[0].paths[0].path="/" \
  --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

echo "Deployment script finished."
