#!/usr/bin/env groovy

def call (String buildStatus = 'STARTED') {
	  // build status of null means successful
	  buildStatus =  buildStatus ?: 'SUCCESSFUL'
	  if (!env.RECIPIENTS){
			env.RECIPIENTS = ''
	  }
	  else{
		if (defaultrecipients != ''){
			RECIPIENTS = ", ${RECIPIENTS}"
		}
	  }
	  // Default values
	  def colorName = 'RED'
	  def colorCode = '#FF0000'
	  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
	  def summary = "${subject} (${env.BUILD_URL})"
	  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
		<p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""
	  def finalrecipients = "${defaultrecipients}${RECIPIENTS}"

	  // Override default values based on build status
	  if (buildStatus == 'STARTED') {
		color = 'YELLOW'
		colorCode = '#FFFF00'
	  } else if (buildStatus == 'SUCCESSFUL') {
		color = 'GREEN'
		colorCode = '#00FF00'
	  } else {
		color = 'RED'
		colorCode = '#FF0000'
	  }

	  // Send notifications
		emailext (
		  subject: subject,
		  body: details,
		  to: "${finalrecipients}"
		)
}
