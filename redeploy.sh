kubectl delete secret agrius-belly-keys
kubectl create secret generic agrius-belly-keys --from-file=keys
kubectl delete -f kube
kubectl apply -f kube
