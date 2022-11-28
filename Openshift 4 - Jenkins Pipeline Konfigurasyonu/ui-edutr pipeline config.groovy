/*
HYPER TECHNOLOGIES TEST ENVIRONMENT APPLICATION PIPELINE v1.0
Application Name : TR-EDU-UI

--------------------------------------------------------------------------------------------------------------------------------------------------------------
SOURCE CODE INFORMATION
--------------------------------------------------------------------------------------------------------------------------------------------------------------
Application GitHub Repository : https://github.com/GlobalHyperSolutions/tr-edu-ui.git
Application API Documentation : 


--------------------------------------------------------------------------------------------------------------------------------------------------------------
DEPLOYMENT ENVIROMENT INFORMATION
--------------------------------------------------------------------------------------------------------------------------------------------------------------
Application Openshift Build Source Brach: origin/master
Application Openshift Route : http://ui-edutr-hyper-applications-edu-tr.apps-crc.testing/
*/

def OC_CLIENT_NAME          = "oc"
def CLUSTER_NAME            = "openshift-local"
def OPENSHIFT_PROJECT_NAME  = "hyper-applications-edu-tr"
def OPENSHIFT_APP_NAME      = "ui-edutr"

def SOURCE_GIT_URL          = "https://github.com/GlobalHyperSolutions/tr-edu-ui.git"
def SOURCE_BRANCH           = "master"

def GRADLE_ARTIFACT_NAME    = "TR-EDU-UI-0.0.1-SNAPSHOT.jar"

pipeline {
    agent any

    //Openshift Client Definition (Check the path in Jenkins => Kontrol Merkezi > Jenkins'i Yönet > Global Tool Configuration => OpenShift Client Tools > Name)
    tools {
        oc OC_CLIENT_NAME
    }

    //Jenkins Gradle Build Path 
    environment{
        GRADLE_USER_HOME = '$WORKSPACE/.gradle'
    }

    //Steps in Pipeline
    stages {

        //Git Clone Step
        stage('Git-Checkout') {
            steps {
                git branch: SOURCE_BRANCH, url: SOURCE_GIT_URL
            }
        }

        //Gradle Build Step
        stage('Gradle Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build'
            }
        }        

        //Build Container Image Step
        stage('Build Container Image') {
            steps {
                script {
                    openshift.withCluster(CLUSTER_NAME) {
                        openshift.withProject(OPENSHIFT_PROJECT_NAME) {
                            def build = openshift
                                    .selector("bc", OPENSHIFT_APP_NAME)
                                    .startBuild("--from-file=build/libs/" + GRADLE_ARTIFACT_NAME,"--follow","--wait=true")

                            build.logs('-f')
                        }
                    }
                }
            }
        }

        //Deployment of Container Image Step
        stage('Deploy Openshift') {
            steps {
                script {
                    openshift.withCluster(CLUSTER_NAME) {
                        openshift.withProject(OPENSHIFT_PROJECT_NAME) {
                            openshift.selector("dc", OPENSHIFT_APP_NAME).rollout().latest();
                        }
                    }
                }
            }
        }

    }
}