

//*********************************************************************** PIPELINE CONFIGURATION STARTS HERE **********************************************************************************





def ENVIRONMENT_TYPE        = "prod"
//-------------------------------------------------------------------------------------------------------------------
//GitHub Repository URL & Branch Name Definitions for Clonning Source Code
def SOURCE_GIT_URL          = "https://github.com/GlobalHyperSolutions/tr-edu-ui.git"
def SOURCE_BRANCH           = "master"
//-------------------------------------------------------------------------------------------------------------------


//-------------------------------------------------------------------------------------------------------------------
//Artifact File Name
def GRADLE_ARTIFACT_NAME    = "TR-EDU-UI-0.0.1-SNAPSHOT.jar"
//-------------------------------------------------------------------------------------------------------------------


//-------------------------------------------------------------------------------------------------------------------
//Sonarqube Installation Name (Check the path in Jenkins => Kontrol Merkezi > Jenkins'i Yönet > Configure System >  SonarQube servers > SonarQube installations > Name)
def SONARQUBE_INSTALLATION_NAME = "sonarqube"

//Sonarqube Project Name
def SONARQUBE_PROJECT_NAME = "ui-edutr"
//-------------------------------------------------------------------------------------------------------------------


//-------------------------------------------------------------------------------------------------------------------
//Openshift Client Definition (Check the path in Jenkins => Kontrol Merkezi > Jenkins'i Yönet > Global Tool Configuration => OpenShift Client Tools > Name)
def OC_CLIENT_NAME          = "oc"

//Openshift Cluster Definition (Check the path in Jenkins => Kontrol Merkezi > Jenkins'i Yönet > Configure System > OpenShift Client Plugin > Cluster Configurations > OpenShift Cluster > Cluster Name)
def CLUSTER_NAME            = "openshift-local"

//Openshift Project Name For Build & Deployment
def OPENSHIFT_PROJECT_NAME  = "hyper-applications-edu-tr"

//Openshift Application Name (BuildConfig/DeploymentConfig name in Openshift Container Platform)
def OPENSHIFT_APP_NAME      = "ui-edutr"
//-------------------------------------------------------------------------------------------------------------------

def NEXUS_VERSION           = "nexus3"
def NEXUS_PROTOCOL          = "http"
def NEXUS_URL               = "nexus3-hyper-devops.apps-crc.testing"
def NEXUS_REPOSITORY        = "EDU-TR-UI"
def NEXUS_CREDENTIAL_ID     = "jenkins-nexus"
def NEXUS_GROUP_ID          = "com.hypertechnologies.edutr." + ENVIRONMENT_TYPE
def NEXUS_ARTIFACT_ID       = NEXUS_REPOSITORY





//*********************************************************************** PIPELINE CONFIGURATION ENDS HERE **********************************************************************************














pipeline {
    agent any

    
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
        
        
                //Code Quality Scan
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv(installationName: SONARQUBE_INSTALLATION_NAME) {
                    sh "./gradlew sonarqube --stacktrace \
                      -Dsonar.projectKey=${SONARQUBE_PROJECT_NAME} \
                      -Dsonar.projectName=${SONARQUBE_PROJECT_NAME} \
                      -Dsonar.userHome=`pwd`/.sonar \
                      -Dsonar.java.source=11 \
                      -Dsonar.projectVersion=${BUILD_NUMBER}"
                }
            }
        }
        
        
        //Wait For Code Quality Scan Result
        stage("Quality Gate") {
            steps {
                waitForQualityGate abortPipeline: true
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

        //Nexus Artifact Uploading
        stage('Store Artifact') {
            steps {
                script {
                    nexusArtifactUploader(
                            nexusVersion: NEXUS_VERSION,
                            protocol: NEXUS_PROTOCOL,
                            nexusUrl: NEXUS_URL,
                            groupId: NEXUS_GROUP_ID,
                            version: 'Version-' + BUILD_NUMBER,
                            repository: NEXUS_REPOSITORY,
                            credentialsId: NEXUS_CREDENTIAL_ID,
                            artifacts: [
                                [
                                    artifactId: NEXUS_ARTIFACT_ID,
                                    classifier: '',
                                    file: 'build/libs/' + GRADLE_ARTIFACT_NAME,
                                    type: "jar"
                                ]
                            ]
                        );
                }
            }
        }

    }
}