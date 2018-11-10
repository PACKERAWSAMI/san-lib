#!/usr/bin/env groovy

//Shared library for Maven build
def call(appname, branchname, reponame) {
		
		stage ('Checkout') { 
			checkout scm
			echo "${branchname}"
		}
		
		stage('Build') {
			def appfileversion = "${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}"
			echo "${appfileversion}"
		    def mvnHome = tool 'maven'
            sh "${mvnHome}/bin/mvn clean package"
			sh "mv target/*.war target/${appfileversion}.war"
		}
	return true
}