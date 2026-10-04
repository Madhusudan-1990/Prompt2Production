pipeline {
agent any

tools {
    maven 'maven'
}

stages {

    stage('Checkout Sample Project') {
        steps {
            dir('sample-project') {
                git 'https://github.com/jglick/simple-maven-project-with-tests.git'
            }
        }
    }

    stage('Build Sample Project') {
        steps {
            dir('sample-project') {
                bat "mvn clean package -Dmaven.test.failure.ignore=true"
            }
        }
    }

    stage('Checkout API Framework') {
        steps {
            dir('api-framework') {
                git branch: 'main', url: 'https://github.com/Madhusudan-1990/Prompt2Production.git'
            }
        }
    }

    stage('E-Commerce API Test - DEV') {
        steps {
            dir('api-framework') {
                script {
                    def status = bat(
                        script: "mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=dev",
                        returnStatus: true
                    )
                    if (status != 0) {
                        currentBuild.result = 'UNSTABLE'
                        echo "DEV failures are expected"
                    }
                }
            }
        }
    }

    stage('E-Commerce API Test - QA') {
        steps {
            dir('api-framework') {
                script {
                    def status = bat(
                        script: "mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=qa",
                        returnStatus: true
                    )
                    if (status != 0) {
                        currentBuild.result = 'UNSTABLE'
                        echo "QA failures are expected"
                    }
                }
            }
        }
    }

    stage('E-Commerce API Test - STAGE') {
        steps {
            dir('api-framework') {
                script {
                    def status = bat(
                        script: "mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=stage",
                        returnStatus: true
                    )
                    if (status != 0) {
                        currentBuild.result = 'UNSTABLE'
                        echo "STAGE failures are expected"
                    }
                }
            }
        }
    }

    stage('E-Commerce API Test - PROD') {
        steps {
            dir('api-framework') {
                bat "mvn clean test -Dsurefire.suiteXmlFiles=src/test/resources/testrunners/testng_ecommerce_regression.xml -Denv=prod"
            }
        }
    }
}

post {
    always {
        echo "Pipeline completed"
    }
    success {
        echo "PROD passed"
    }
    unstable {
        echo "Non-prod failures occurred (expected)"
    }
    failure {
        echo "PROD failed"
    }
}


}
