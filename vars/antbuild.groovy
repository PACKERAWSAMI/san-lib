#!/usr/bin/env groovy

//Shared library for Maven build
def call(appname, branchname, buildfile) {
		
		stage('Build') {
			def appfileversion = "${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}"
			echo "${appfileversion}"
            sh "${ANT_HOME}/bin/ant -buildfile ${buildfile}"
			sh "mkdir -p target"
			sh "cp ${WARPATH}/*.war target/${appfileversion}.war"
			env.artifactName = "${appfileversion}.war"
		}
	return true
}
