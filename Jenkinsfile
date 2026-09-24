// This is the Jenkinsfile of the m2e-2.x branch: only builds of this exact branch (not PRs/topic branches against it) are signed/released
def isReleaseBranch = 'm2e-2.x'.equals(env.BRANCH_NAME)

pipeline {
	options {
		timeout(time: 45, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr: isReleaseBranch ? '20' : '5', artifactNumToKeepStr: isReleaseBranch ? '5' : '1' ))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label 'ubuntu-latest'
	}
	tools {
		maven 'apache-maven-3.9.16'
		jdk 'temurin-jdk21-latest'
	}
	stages {
		stage('get m2e-core-tests') {
			steps {
				sh 'git submodule update --init --recursive --remote'
			}
		}
		stage('Build') {
			steps {
				withCredentials([
					file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING'),
					string(credentialsId: 'gpg-passphrase', variable: 'MAVEN_GPG_PASSPHRASE')
				]) {
				xvnc(useXauthority: true) {
					sh '''#!/bin/bash -x
						mavenArgs="clean verify --batch-mode -Dmaven.test.failure.ignore=true -Dtycho.p2.baselineMode=failCommon"
						if [[ ${BRANCH_NAME} == m2e-2.x ]]; then
							mvn ${mavenArgs} -Peclipse-sign,its -Dtycho.pgp.signer.bc.secretKeys="${KEYRING}"
						else
							# Clear signing environment variables for PRs
							export KEYRING='EMPTY'
							export MAVEN_GPG_PASSPHRASE='EMPTY'
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
					junit '*/target/surefire-reports/TEST-*.xml,*/*/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'm2e/m2e-2.x'
					recordIssues enabledForFailure: true, publishAllIssues: true, ignoreQualityGate: true, tools: [
							eclipse(name: 'Compiler', pattern: '**/target/compilelogs/*.xml'),
							mavenConsole(),
							javaDoc()
						], qualityGates: [[threshold: 1, type: 'NEW', unstable: true]]
				}
			}
		}
		stage('Deploy Snapshot') {
			when {
				branch 'm2e-2.x'
			}
			steps {
				sshagent(['projects-storage.eclipse.org-bot-ssh']) {
					sh '''#!/bin/bash -x
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
						# m2e-2.x is a maintenance branch: keep its own versioned snapshot path, don't touch the "latest" pointer (that's main's)
						deployM2ERepository /home/data/httpd/download.eclipse.org/technology/m2e/snapshots/${M2E_VERSION}
					'''
				}
			}
		}
	}
}
