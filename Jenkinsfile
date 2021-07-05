pipeline {
	options {
		timeout(time: 180, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk11-latest'
	}
	stages {
		stage('get m2e-core-tests') {
			steps {
				sh 'git submodule update --init --recursive --remote'
			}
		}
		stage('Build') {
			steps {
				sh 'mvn clean install -f m2e-maven-runtime/pom.xml -B -Peclipse-sign -Dmaven.repo.local=$WORKSPACE/.m2/repository'
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -f pom.xml -B -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Peclipse-sign,uts,its -Dmaven.repo.local=$WORKSPACE/.m2/repository -Dtycho.surefire.timeout=7200'
				}
			}
			post {
				always {
					archiveArtifacts artifacts: 'org.eclipse.*.site/target/repository/**/*,org.eclipse.*.site/target/*.zip,*/target/work/data/.metadata/.log,m2e-core-tests/*/target/work/data/.metadata/.log,m2e-maven-runtime/target/*.properties'
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
							ssh genie.m2e@build.eclipse.org "\
								rm -rf  ${1}/* && \
								mkdir -p ${1}"
							scp -r org.eclipse.m2e.site/target/repository/* genie.m2e@build.eclipse.org:${1}
						}
						M2E_VERSION=$(grep '<m2e.version>.*</m2e.version>' pom.xml | sed -e 's/.*<m2e.version>\\(.*\\)<\\/m2e.version>.*/\\1/')
						SNAPSHOT_VERSIONED_AREA=/home/data/httpd/download.eclipse.org/technology/m2e/snapshots/${M2E_VERSION}/latest
						SNAPSHOT_LATEST_AREA=/home/data/httpd/download.eclipse.org/technology/m2e/snapshots/latest

						echo M2E_VERSION=$M2E_VERSION
						deployM2ERepository $SNAPSHOT_VERSIONED_AREA
						deployM2ERepository $SNAPSHOT_LATEST_AREA
					'''
				}
			}
		}
	}
}
