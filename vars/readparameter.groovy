#!/usr/bin/env groovy

//Shared library for build
def call(serverpath, propertypath, deployenv) {

	stage ('getserverdata'){
		//reading the server details from the server.properties
		def serverprop = readProperties  file: "${workspace}/${serverpath}/server.properties"
		
		//get the deploy type from the server.properties file
		env.DEPLOYTYPE = serverprop['deploytype']
		env.SERVERNAME = serverprop[deployenv]
		env.REPO = serverprop['reponame']
		env.GROUP = serverprop['groupname']
		env.RECIPIENTS = serverprop['RECIPIENTS']
		env.EMAILRECIPIENTS = serverprop['RECIPIENTS']

		echo "${DEPLOYTYPE}"
	
		switch ("${DEPLOYTYPE}"){
			case "tomcat":
				
				def deployport = "${deployenv}.port"
				def deployinstance = "${deployenv}.instance"
                def tomcatpath = "${propertypath}/Systemfiles"
				//read the tomcat properties file
				readtomcat "${tomcatpath}"
				
				//read the tomcat port
				env.PORT = serverprop[deployport]
				env.TOMCATINSTANCE = serverprop[deployinstance]
				env.TOMCATAPPNAME = serverprop['tomcat.appname']
				
				//setup Tocat variables
				env.TOMCAT_URL="http://${SERVERNAME}:${PORT}"
				env.TOMCAT_WEB_MANAGER="${TOMCAT_URL}/manager/html"
				env.CATALINA_BASE="${CATALINABASE}/${TOMCATINSTANCE}"
				env.TOCAT_SYSTEM_LOG="${CATALINA_BASE}/${CATALINALOG}"
				env.TOMCAT_INSTANCE_PID_FILE="${CATALINA_BASE}/temp/${TOMCATINSTANCE}.pid"
				env.TOMCAT_USERNAME='scdm'
				env.TOMCAT_PASSWORD='kittycat'
				env.TOMCAT_WEBAPPS="${CATALINA_BASE}/webapps"
				env.TOMCAT_MANAGER_URL="${TOMCAT_URL}/manager/html"
				
				break

			case "CopyToShare":
			
				env.CONTAINER_APP_NAME = serverprop['Container.appname']
				env.UNZIP_TO_DESTINATION = serverprop['unzip_destination']
				env.SERVICE_LIST = serverprop['servicelist']
				env.SERVICE_TYPE = serverprop['servicetype']
				env.SERVICESTARTSTOP = serverprop['stopstartservice']
				break;

			case "default":
				echo 'Deploytype is null'
				return false
		}
		
		
	}
		
	return true
}
