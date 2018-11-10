#!/usr/bin/env groovy

//Shared library for build
def call(tomcatpath) {

	stage ('gettomcatdata'){
		//reading the server details from the server.properties
		def tomcatprop = readProperties  file: "${workspace}/${tomcatpath}/tomcat.properties"
		
		//get the deploy type from the server.properties file
		env.CATALINABASE = tomcatprop['catalina.base']
		env.CATALINAHOME = tomcatprop['catalina.home']
		env.CATALINALOG = tomcatprop['catalina.log']
		
		env.TOMCATMEMORY = tomcatprop['tomcat.initial.memory']
		env.TOMCATMAXMEM = tomcatprop['tomcat.mx.memory']
		env.TOMCATPERMSIZE = tomcatprop['tomcat.max.permsize']	
		env.TOMCATDEPLOYER = tomcatprop['tomcat.deployer']
		
		echo "${CATALINABASE}"
	}
		
	return true
}
