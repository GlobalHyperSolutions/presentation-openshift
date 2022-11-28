# Cluster üzerindeki yetki verilen projelerin listesini görme
oc get projects

# OC Client komutları için kullanacağınız projeyi değiştirme
oc project [PROJECT_NAME]

# BuildConfig Oluşturma
oc new-build --name=ui-edutr --image-stream=java:11 --binary=true -n hyper-applications-edu-tr

# Bir BuildConfig üzerinden binary bir artifact ın container imajını oluşturma
oc start-build ui-edutr --from-file=C:\LAB\TR-EDU-UI-0.0.1-SNAPSHOT.jar --follow

# Bir BuildConfig üzerinden DeploymentConfig oluşturmak ve bu DeploymentConfig üzerinden POD ayağa kaldırmak
oc new-app ui-edutr:latest -l app=ui-edutr -n hyper-applications-edu-tr --as-deployment-config

# Bir servisin router'ını oluşturmak
oc expose svc/ui-edutr

# Bulunulan projedeki podların listesini görme
oc get pods

# Bulunulan projedeki bir pod un yaml tanımını görme
oc get pod/[POD_NAME] -o yaml

# Belirli bir pod'un loglarını sürekli takip modunda görüntüleme
oc logs pod/[POD_NAME] --follow

# Bulunulan projedeki servis listesini (network) görme
oc get svc

# Bulunulan projedeki bir servisin yaml tanımını görme
oc get svc/[SERVICE_NAME] -o yaml

# Bulunulan projedeki bir route listesini (network) görme
oc get route

# Bulunulan projedeki bir route un yaml tanımını görme
oc get route/[ROUTE_NAME] -o yaml

# Bulunulan projedeki container imajların listesini görme
oc get is

# Bulunulan projedeki bir container imajının yaml tanımını görme
oc get is/[IMAGE_STREAM_NAME] -o yaml