kubectl get pods -n prod

helm uninstall notification-service -n prod

helm install customer-service my-microservices-app/charts/customer-service --namespace prod --create-namespace --set image.repository=customer-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=customer.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install fraud-service my-microservices-app/charts/fraud-service --namespace prod --create-namespace --set image.repository=fraud-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=fraud.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install notification-service my-microservices-app/charts/notification-service --namespace prod --create-namespace --set image.repository=notification-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=notification.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install exchange-service my-microservices-app/charts/exchange-service --namespace prod --create-namespace --set image.repository=exchange-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=exchange.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install exchange-generator-service my-microservices-app/charts/exchange-generator-service --namespace prod --create-namespace --set image.repository=exchange-generator-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=exchange-generator.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install cash-service my-microservices-app/charts/cash-service --namespace prod --create-namespace --set image.repository=cash-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=cash.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install transfer-service my-microservices-app/charts/transfer-service --namespace prod --create-namespace --set image.repository=transfer-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=transfer.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install front-ui-service my-microservices-app/charts/front-ui-service --namespace prod --create-namespace --set image.repository=front-ui-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=front-ui.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"

helm install apigw-service my-microservices-app/charts/apigw-service --namespace prod --create-namespace --set image.repository=apigw-service --set image.tag=latest --set ingress.enabled=true --set ingress.hosts[0].host=apigw.prod.local --set ingress.hosts[0].paths[0].path="/" --set ingress.hosts[0].paths[0].pathType="ImplementationSpecific"