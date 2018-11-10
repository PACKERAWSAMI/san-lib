#!/usr/bin/env groovy

def returncode = 0
//Shared library for build
def call(deployenv, appName, branch, artifactName) {
	stage ('CopyToShare'){
		
		sh "ping -c1 ${SERVERNAME}"

		//check if the unzip destination available
		checkdestination()

		deletedestinationfiles()

		copyzip("${appName}", "${branch}", "${artifactName}")

		unzip("${appName}", "${branch}", "${artifactName}")

		deletezip()
	}
		
	return true
}


def checkdestination() {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
	

			def COMMANDS_TO_RUN = "ls ${UNZIP_TO_DESTINATION}"
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}"
	}

	return true
}

def deletedestinationfiles() {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
	

			def COMMANDS_TO_RUN = "rm -rf  ${UNZIP_TO_DESTINATION}/*"
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}"
	}

	return true
}

def copyzip(appName, branch, artifactName) {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
	

			sh "sshpass -p '${DEPLOY_PASS}' scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null download/artifact/${appName}/${branch}/${artifactName} ${DEPLOY_USER}@${SERVERNAME}:${UNZIP_TO_DESTINATION}"

	}

	return true
}

def unzip(appName, branch, artifactName) {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
	

			

			def COMMANDS_TO_RUN = "unzip -o ${UNZIP_TO_DESTINATION}/${artifactName} -d ${UNZIP_TO_DESTINATION}/"
			
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}"
	}

	return true
}

def deletezip() {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
	

			def COMMANDS_TO_RUN = "rm -f ${UNZIP_TO_DESTINATION}/${artifactName}"
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}"
	}

	return true
}

