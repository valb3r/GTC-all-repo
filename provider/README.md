# gtc

GCE deployment:

1. ./gradlew clean build
2. docker build . -t gcr.io/bidcache/bidcache:$VERSION$
3. gcloud docker -- push gcr.io/bidcache/bidcache:$VERSION$
4. update deploy.yaml with version ($VERSION$):
5. kubectl apply -f /home/valb3r/Documents/projects/gtc/gce/deploy.yaml

GCE port exposure:
1. Get svc name: kubectl get service
2. Expose: kubectl expose deployment bid-cacher --type=LoadBalancer --port 15005 --target-port 15005

Creating secrets:
kubectl create secret generic newrelic-credentials --from-literal=license=***

kubectl create secret generic cloudsql-db-credentials \
    --from-literal=username=<PROXY_USR> --from-literal=password=<PROXY_PASSW> \
    --from-literal=dburl=127.0.0.1:3306/<DBNAME> --from-literal=dbname=<DBNAME>
