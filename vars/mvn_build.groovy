#!/usr/bin/env groovy

//Shared library for Maven build
def call(appname, branchname) {
	def returnVal
	def String mvngoals
	def appfileversion
	stage('Build') {

			//check if appExt is set
			if (!env.appExt){
				env.appExt = 'null'
			}

			//check and set the maven goal list to default if not set from Jenkinsfile
			if (!env.MAVEN_GOAL_LIST){
				env.MAVEN_GOAL_LIST = 'clean compile package'
			}
			else{
				returnVal = sh(script:"echo ${MAVEN_GOAL_LIST} | grep -i DskipTests=true", returnStatus:true)
				if (!returnVal){
					mvngoals = "${MAVEN_GOAL_LIST}"
				}
				else {
					mvngoals =  "${MAVEN_GOAL_LIST} -DskipTests=true"
				}
			}

			echo "MAVEN_GOAL_LIST: ${mvngoals}"

			returnVal = sh(script:" cat pom.xml|grep -i maven.test.skip|grep -i false", returnStatus:true)
			if (!returnVal){
				env.UNITTEST='true'
			}
			else{
				env.UNITTEST = 'false'
			}

			echo "UNITTEST: ${UNITTEST}"

			//setting the artifact file name to appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}
			appfileversion = "${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}"

			//compile
		    def mvnHome = tool 'maven'
            sh "${mvnHome}/bin/mvn ${mvngoals}"
	}

	if (UNITTEST == 'true'){
		stage ('Unit Test'){
			try{
				def mvnHome = tool 'maven'
				sh "${mvnHome}/bin/mvn test"
			}
			catch (err){
				env.JUNITSTATE = 'Fail'
				echo "====================================="
				echo " Unit test failed"
				echo "JUNITSTATE: ${JUNITSTATE}"
				echo "====================================="

				//throw err
				currentBuild.result = 'UNSTABLE'
			}
			finally{
				//junit 'testresults/*xml'
				if (UNITTEST == 'true'){
					step([$class: 'JUnitResultArchiver',
							testResults: 'target/surefire-reports/*xml'])
				}
				if(!env.JUNITSTATE){
					env.JUNITSTATE = 'Pass'
				}

				echo "JUNITSTATE: ${JUNITSTATE}"
			}
		}
	}
	else {
		env.JUNITSTATE = 'Pass'
	}

	if (JUNITSTATE == 'Fail'){
		echo "JUNITSTATE: ${JUNITSTATE}"
		return
	}

	stage ('compress') {
			//change the target artifact into the SA naming format
			if (appExt == 'zip'){
				//renaming the zip file with the version and timestamp
				sh "mv target/*.zip target/${appfileversion}.zip"
				env.artifactName = "${appfileversion}.zip"
			}
			else if (appExt == 'jar'){
				//renaming the war file with the version and timestamp
				sh "mv target/*.jar target/${appfileversion}.jar"
				env.artifactName = "${appfileversion}.jar"
				sh "rm -rf target/repository"
			}
			else {
				//renaming the war file with the version and timestamp
				sh "mv target/*.war target/${appfileversion}.war"
				env.artifactName = "${appfileversion}.war"
			}

			//Setting the global artifact name to be used for auto deploy job

	}
	return true
}
