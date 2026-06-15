pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK21'
    }

    environment {
        JMETER_HOME = '/jmeter'
        JMETER_BIN  = '/jmeter/bin/jmeter.sh'
        REPORT_DIR  = 'jmeter-report'
    }

    stages {

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Tests') {
            steps {
                sh 'mvn test -P ci'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                sh 'mvn verify -P ci'
            }
            post {
                always {
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }

        stage('Load Test - JMeter') {
            steps {
                sh """
                    rm -rf ${REPORT_DIR}
                    mkdir -p ${REPORT_DIR}/html
                    ${JMETER_BIN} -n -t load-test.jmx -l ${REPORT_DIR}/results.jtl -e -o ${REPORT_DIR}/html
                """
            }
            post {
                always {
                    perfReport filterRegex: '',
                              sourceDataFiles: "${REPORT_DIR}/results.jtl"
                }
                success {
                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: "${REPORT_DIR}/html",
                        reportFiles: 'index.html',
                        reportName: 'JMeter Load Test Report'
                    ])
                }
            }
        }
    }

    post {
        failure {
            echo "Build echoue : ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${env.BUILD_URL}"
        }
        success {
            echo 'Build, Tests et Load Test reussis !'
        }
    }
}
