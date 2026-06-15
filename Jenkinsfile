pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK21'
    }

    environment {
        JMETER_HOME = 'C:\\jmeter'
        JMETER_BIN  = "${JMETER_HOME}\\bin\\jmeter.bat"
        REPORT_DIR  = 'jmeter-report'
    }

    stages {

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                bat 'mvn test -pl . -Dtest="**/junit_test/**"'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('MockMvc Tests') {
            steps {
                bat 'mvn test -Dtest="**/mockmvc_test/**"'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                bat 'mvn verify -DskipTests=false'
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
                bat """
                    if exist ${REPORT_DIR} rmdir /s /q ${REPORT_DIR}
                    mkdir ${REPORT_DIR}
                    ${JMETER_BIN} -n -t load-test.jmx -l ${REPORT_DIR}\\results.jtl -e -o ${REPORT_DIR}\\html
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
            mail to: 'laklatyanwar@gmail.com',
                 subject: "Build échoué : ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: """
                     Le build ${env.JOB_NAME} #${env.BUILD_NUMBER} a échoué.
                     Voir les détails : ${env.BUILD_URL}
                 """
        }
        success {
            echo 'Build, Tests et Load Test réussis !'
        }
    }
}
