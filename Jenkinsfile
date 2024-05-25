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
		maven 'apache-maven-3.9.6'
		jdk 'openjdk-jdk21-latest'
	}
	stages {
		stage('get m2e-core-tests') {
			steps {
				sh 'git submodule update --init --recursive --remote'
			}
		}
		stage('initialize PGP') {
			when {
				anyOf{
					branch 'master';
					branch pattern: 'm2e-[0-9]+\\.[0-9]+\\.x', comparator: "REGEXP"
				}
			}
			steps {
				withCredentials([file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING')]) {
					sh 'gpg --batch --import "${KEYRING}"'
					sh '''
						for fpr in $(gpg --list-keys --with-colons | awk -F: \'/fpr:/ {print $10}\' | sort -u)
						do
							echo -e "5\ny\n" | gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust
						done
					'''
				}
			}
		}
		stage('Build') {
			steps {
				withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'KEYRING_PASSPHRASE')]) {
				xvnc(useXauthority: true) {
					sh '''
						mavenArgs="clean verify --batch-mode -Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true -Dtycho.p2.baselineMode=failCommon"
						if [[ ${BRANCH_NAME} == master ]] || [[ ${BRANCH_NAME} =~ m2e-[0-9]+\\.[0-9]+\\.x ]]; then
							mvn ${mavenArgs} -Peclipse-sign,its -Dgpg.passphrase="${KEYRING_PASSPHRASE}" -Dgpg.keyname="011C526F29B2CE79"
						else
							# Clear KEYRING_PASSPHRASE environment variable
							export KEYRING_PASSPHRASE='EMPTY'
							mvn ${mavenArgs} -Pits
						fi
					'''
				}}
			}
			post {
				always {
					archiveArtifacts artifacts: 'org.eclipse.m2e.repository/target/*.zip,\
						*/target/work/data/.metadata/.log,\
						m2e-core-tests/*/target/work/data/.metadata/.log,\
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
								rm -rf ${1}/* && \
								mkdir -p ${1}"
							scp -r org.eclipse.m2e.repository/target/repository/* genie.m2e@projects-storage.eclipse.org:${1}
						}
						# Read M2E branding version
						version=$(xmllint --xpath 'string(/feature/@version)' org.eclipse.m2e.sdk.feature/feature.xml)
						if [[ $version =~ ([0-9]+\\.[0-9]+\\.[0-9]+)\\.qualifier ]] # backslash itself has to be escaped in Jenkinsfile
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
