#!/usr/bin/env groovy

def returncode = 0
//Shared library for build
def call(deployenv, appName, branch, artifactName) {
	ant = tool name: 'ant-1.8.2'
	env.ANTHOME = "${ant}"
	stage ('tomcatdeploy'){
		//check the server is reachable
		//returncode = sh script:"ping -c1 ${SERVERNAME}", returnStatus:true
		//if (returncode) {
		//	echo "${SERVERNAME} is not reachable. Please reach WEBIS"
		//	return false
		//}
		//echo "${returncode}"
		
		sh "ping -c1 ${SERVERNAME}"

		//check if application available in webapps
		tomcatoperation("checkapp")
		echo "App status is ${APPAVAIL}"
		if (APPAVAIL == "0"){
			//check the tomcat instance is already running
			//checkTomcatInstance()
			tomcatoperation("check")
			

			if (PROCESSVAL) {

				echo 'The Process ID is running'
				//undeploy the app - whicg stop the app and undeploy the war
				tomcatoperation("undeploy")

			}
			else {
				//Start the tomcat instance and the app
				tomcatoperation("start")
				
				//undeploy the app - which stop the app and undeploy the war
				tomcatoperation("undeploy")
			}
			
			tomcatoperation("stopjvm")

		}
			
		


		//Copy the file
		withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
					
			
			
            def COMMANDS_TO_RUN = "sudo /etc/init.d/${TOMCATINSTANCE} stop"
			sh "sshpass -p '${DEPLOY_PASS}' scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null download/artifact/${appName}/${branch}/${artifactName} ${DEPLOY_USER}@${SERVERNAME}:/${TOMCAT_WEBAPPS}/${TOMCATAPPNAME}.war"
			//${DEPLOY_USER}@${SERVERNAME}:${TOMCAT_WEBAPPS}/${TOMCATAPPNAME}.war"
			
		}

		//Start the app
		tomcatoperation("start")

	}
		
	return true
}

def tomcatoperation(operation){
	
		switch ("${operation}"){
			case "stop":
				//Stop the app first
				stopTomcatapp()
				//Stop the server
				stopTomcatInstance()
				break
			case "start":
				//Start the server
				startTomcatInstance()

				sleep(20)
				//Start the app
				startTomcatapp()
				break
			case "check":
				//Start the server
				checkTomcatInstance()
				break
			case "restart":
				//stop the server
				break		
			case "stopjvm":
				//stop the jvm
				stopTomcatInstance()
				break
			case "checkapp":
				checkinstance()
				break
			case "undeploy":
				//delete the app
				//Stop the app first
				stopTomcatapp()
				
				undeployTomcatapp()
				
				
			default: 
				echo 'Pleae mention start/stop/restart'
		}

	return true
	
}

def stopTomcatInstance() {

	//Stop the tomcat 
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
					
			
			
            def COMMANDS_TO_RUN = "sudo /etc/init.d/${TOMCATINSTANCE} stop"
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}" 
			
	}

	
	return true
}

def stopTomcatapp() {

	def tomcatapppath = "/${TOMCATAPPNAME}"
	echo "/${TOMCATAPPNAME}"

	//Stop with tomcat app first
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "tomcatmanager",
								usernameVariable: 'TOMCAT_USER',
								passwordVariable: 'TOMCAT_PASS']]) {


			sh "${ANTHOME}/bin/ant -Dusername=${TOMCAT_USER} -Dpassword=${TOMCAT_PASS} -file ${TOMCATDEPLOYER}/build.xml -lib ${TOMCATDEPLOYER}/lib -Durl=${TOMCAT_URL}/manager/text -Dwebapp=${TOMCATAPPNAME} -Dpath=${tomcatapppath} stop"
	}

	return true

}

def startTomcatInstance() {

	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
					
			
			
            def COMMANDS_TO_RUN = "sudo /etc/init.d/${TOMCATINSTANCE} start"
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}"

	}

	return true
}

def startTomcatapp() {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "tomcatmanager",
								usernameVariable: 'TOMCAT_USER',
								passwordVariable: 'TOMCAT_PASS']]) {
	

			sh "${ANTHOME}/bin/ant -Dusername=${TOMCAT_USER} -Dpassword=${TOMCAT_PASS} -file ${TOMCATDEPLOYER}/build.xml -lib ${TOMCATDEPLOYER}/lib -Durl=${TOMCAT_URL}/manager/text -Dwebapp=${TOMCATAPPNAME} -Dpath=/${TOMCATAPPNAME} start"
	}

	return true
}

def checkTomcatInstance(){

	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
					
			def awkcommand = "awk '{print \$2}'"
			
            def COMMANDS_TO_RUN = "ps -ef | grep ${TOMCATINSTANCE} | grep -v grep | ${awkcommand}"
			//sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} '${COMMANDS_TO_RUN}'"
			env.PROCESSVAL = sh(
				script: "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}",
				returnStdout: true
			)

			echo "ps result is ${PROCESSVAL}"
	}

	return true

}

def undeployTomcatapp() {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "tomcatmanager",
								usernameVariable: 'TOMCAT_USER',
								passwordVariable: 'TOMCAT_PASS']]) {
	

			sh "${ANTHOME}/bin/ant -Dusername=${TOMCAT_USER} -Dpassword=${TOMCAT_PASS} -file ${TOMCATDEPLOYER}/build.xml -lib ${TOMCATDEPLOYER}/lib -Durl=${TOMCAT_URL}/manager/text -Dwebapp=${TOMCATAPPNAME} -Dpath=/${TOMCATAPPNAME} undeploy"
	}

	return true
}

def checkinstance() {
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
	

			def COMMANDS_TO_RUN = "ls ${TOMCAT_WEBAPPS}/${TOMCATAPPNAME}"
			env.APPAVAIL = sh(
				script: "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${COMMANDS_TO_RUN}",
				returnStatus:true
			)
	}

	return true
}


