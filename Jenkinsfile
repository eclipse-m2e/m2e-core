pipeline {
	options {
		timeout(time: 45, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-3.8.6'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('get m2e-core-tests') {
			steps {
				sh 'git submodule update --init --recursive --remote'
			}
		}
		stage('Build') {
			steps {
				sh 'mvn clean generate-sources -f m2e-maven-runtime/pom.xml -B -Dtycho.mode=maven -Pgenerate-osgi-metadata'
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -f pom.xml -B -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Peclipse-sign,its -Dtycho.surefire.timeout=7200'
				}
			}
			post {
				always {
					archiveArtifacts artifacts: 'org.eclipse.m2e.repository/target/*.zip,\
						*/target/work/data/.metadata/.log,\
						m2e-core-tests/*/target/work/data/.metadata/.log,\
						m2e-maven-runtime/target/*.properties,\
						**/target/artifactcomparison/*'
					archiveArtifacts (artifacts: '**/target/products/*.zip,**/target/products/*.tar.gz', onlyIfSuccessful: true)
					junit '*/target/surefire-reports/TEST-*.xml,*/*/target/surefire-reports/TEST-*.xml'
				}
			}
		}
		stage('Deploy Snapshot') {
			when {
				branch 'master'
			}
			steps {
				sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						deployM2ERepository()
						{
							echo Deploy m2e repo to ${1}
							ssh genie.m2e@projects-storage.eclipse.org "\
								rm -rf  ${1}/* && \
								mkdir -p ${1}"
							scp -r org.eclipse.m2e.repository/target/repository/* genie.m2e@projects-storage.eclipse.org:${1}
						}
						# Read M2E branding version
						version=$(xmllint --xpath 'string(/feature/@version)' org.eclipse.m2e.sdk.feature/feature.xml)
						if [[ $version =~ ([0-9]\\.[0-9]\\.[0-9])\\.qualifier ]] # backslash itself has to be escaped in Jenkinsfile
						then
							M2E_VERSION="${BASH_REMATCH[1]}"
						else
							echo Failed to read M2E_VERSION. Abort deployment.
							exit 1
						fi
						deployM2ERepository /home/data/httpd/download.eclipse.org/technology/m2e/snapshots/${M2E_VERSION}
						deployM2ERepository /home/data/httpd/download.eclipse.org/technology/m2e/snapshots/latest
					'''
				}
			}
		}
	}
}
