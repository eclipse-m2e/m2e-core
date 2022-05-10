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
		jdk 'temurin-jdk11-latest'
	}
	stages {
		stage('get m2e-core-tests') {
			steps {
				sh 'git submodule update --init --recursive --remote'
			}
		}
		stage('Build') {
			steps {
				sh 'mvn clean generate-sources -f m2e-maven-runtime/pom.xml -B -Dtycho.mode=maven -Pgenerate-osgi-metadata '
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh 'mvn clean verify -f pom.xml -B -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Peclipse-sign,uts,its -Dtycho.surefire.timeout=7200'
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
							ssh genie.m2e@projects-storage.eclipse.org "\
								rm -rf  ${1}/* && \
								mkdir -p ${1}"
							scp -r org.eclipse.m2e.repository/target/repository/* genie.m2e@projects-storage.eclipse.org:${1}
						}
						# Read M2E branding version
						regex='<feature id="org\\.eclipse\\.m2e\\.sdk\\.feature" version="([0-9]\\.[0-9]\\.[0-9])\\.qualifier" '
						content=$(echo $(<"org.eclipse.m2e.sdk.feature/feature.xml")) # replaces consecutive newline and tabs by single space
						if [[ $content  =~ $regex ]]
						then
							M2E_VERSION="${BASH_REMATCH[1]}"
							echo M2E_VERSION=$M2E_VERSION
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
