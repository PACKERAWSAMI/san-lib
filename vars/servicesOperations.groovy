#!/usr/bin/env groovy

def call(operation, appname) {
	def appName="${appname}"
	//def returnval
	def servicecommand
	def checkcommand 
	def servicestatestop
	def servicestatestart
	def boolean statuscode
	
	stage("${operation}"){
		if (appName == 'Sails'){
			servicestatestop = 'stopped process'
			servicestatestart = 'processing file'
		}
		else if (appName == 'AuthorizationService'){
			servicestatestop = 'Service not running'
			servicestatestart = 'Running'
		}
		else {
			servicestatestop = 'Stopped'
			servicestatestart = 'Started'
		}
		
		
		
		switch ("${operation}"){
			case "stop":
				if (SERVICE_TYPE == 'tomcat'){
					servicecommand = "sudo etc/init.d/${SERVICE_LIST} ${operation}"
					checkcommand = "sudo etc/init.d/${SERVICE_LIST} status"
				}
				else if (SERVICE_TYPE == 'httpd'){
					servicecommand = "sudo service ${SERVICE_LIST} ${operation}"
					checkcommand ="sudo service ${SERVICE_LIST} status -l |  grep -e ${servicestatestop}"
				}
				
				startstopService("${servicecommand}")
				
				//check if the tomcat is stopped
				statuscode = checkTomcatInstance("${checkcommand}", "${servicestatestop}")
				
				if (!statuscode){
					echo "Service Stop failed"
					env.SERVICE_STATE = 'fail'
				}
				else {
					env.SERVICE_STATE = 'pass'
				}
				
				break
				
			case "start":
				if (SERVICE_TYPE == 'tomcat'){
					servicecommand = "sudo etc/init.d/${SERVICE_LIST} ${operation}"
					checkcommand = "sudo etc/init.d/${SERVICE_LIST} status"
				}
				else if (SERVICE_TYPE == 'httpd'){
					servicecommand = "sudo service ${SERVICE_LIST} ${operation}"
					checkcommand ="sudo service ${SERVICE_LIST} status -l |  grep -e ${servicestatestart}"
				}
				
				echo "checkcommand: ${checkcommand}"
				
				startstopService("${servicecommand}")
				
				//sleep(4)
				
				//check if the tomcat is stopped
				statuscode = checkTomcatInstance("${checkcommand}", "${servicestatestart}")
				
				if (!statuscode){
					echo "Service Stop failed"
					env.SERVICE_STATE = 'fail'
				}
				else {
					env.SERVICE_STATE = 'pass'
				}
				
				
				break
			
			default: 
				echo 'Pleae mention start/stop/restart'
		}
		
	}
	
	
	
	
	return true
}

def checkTomcatInstance(checkcommand, servicestatemsg){
	def String returnval
	def boolean returncode
	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
					
						
            returnval = sh(
				script: "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${checkcommand}",
				returnStdout: true
			)
			
			echo "${returnval}"
			
			returncode = returnval.contains("${servicestatemsg}")
	}

	return returncode

}


def startstopService(servicecommand) {

	withCredentials([[$class: 'UsernamePasswordMultiBinding',
								credentialsId: "${DEPLOY_ID}",
								usernameVariable: 'DEPLOY_USER',
								passwordVariable: 'DEPLOY_PASS']]) {
					
			
			sh "sshpass -p '${DEPLOY_PASS}' ssh  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ${DEPLOY_USER}@${SERVERNAME} ${servicecommand}"

	}

	return true
}




