#!/usr/bin/env groovy

//Shared library for build
def call(appname, buildtype) {
	env.defaultrecipients=''
	env.APPNAME="${appname}"
	def String [] jobname = JOB_NAME.split("/")
	env.CONSOLE_OUT="http://l5-jenkins.ent.stateauto.com:8080/job/${jobname[0]}/job/${jobname[1]}/${BUILD_NUMBER}/console"
	try {
		//Checkout from version control
		stage ('Checkout') {
			checkout scm
			if (CODEREPO=="git"){
				final scmVars = checkout(scm)
				//echo "scmVars: ${scmVars}"
				//echo "scmVars.GIT_BRANCH: ${scmVars.GIT_BRANCH}"
				env.branchname = "${scmVars.GIT_BRANCH}"
				echo "branchname is: ${scmVars.GIT_BRANCH}"

				def String GIT_URL = "${scmVars.GIT_URL}"
				def String[] URL_git = GIT_URL.split('//')
				def String url_noHttps = URL_git[1]
				env.GITURL_nohttps = "${url_noHttps}"
				env.SCM_URL = "${GIT_URL}"


			}
			if (CODEREPO=="svn"){
				def remote = scm.locations.first().remote
				def url = remote.split('@').first()
				env.SVN_URL = "${url}"
				echo "${SVN_URL}"
				def String[] branch = env.BRANCH_NAME.tokenize('/')
				def String branch_name = branch[1]
				env.branchname = "${branch_name}"
				echo "branchname is: ${branchname}"

				env.SCM_URL = "${SVN_URL}"
			}

			env.BRANCH_NAME = "${branchname}"
		}
		stage ('tag'){
		 	//check if DEPLOY_OS is set
			if (!env.DEPLOY_OS) {
				env.DEPLOY_OS = 'linux'
			}
		 	//if (DEPLOY_OS != "windows"){
		 		if (CODEREPO == "svn"){
		 			tagsvnbuild "${appname}", "${branchname}"
		 		}
		 		else {
		 			taggitbuild "${appname}", "${branchname}"
		 		}
		 	//}
		 }

		//Stage for Code check
		stage('SonarQube') {
			if (!env.CODEQUAL){
				env.CODEQUAL = 'false'
			}
			if (CODEQUAL == "true") {
				switch ("${buildtype}"){
					case "maven":
						withSonarQubeEnv('SonarQube_new') {
							jdk = tool name: 'JDK8'
							env.JAVA_HOME = "${jdk}"
							echo "jdk path is: ${JAVA_HOME}"
						  def mvnHome = tool 'maven'
						  sh "${mvnHome}/bin/mvn clean compile org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar"
						}
						break
					case "angular" :
						def scannerHome = tool 'SonarScanner'
						withSonarQubeEnv('SonarQube_new') {
							bat "${scannerHome}/bin/sonar-scanner"
						}
						break
					case "ant" :
						def scannerHome = tool 'SonarScanner'
						withSonarQubeEnv('SonarQube_new') {
							jdk = tool name: 'JDK8'
							env.JAVA_HOME = "${jdk}"
							echo "jdk path is: ${JAVA_HOME}"
							sh "${scannerHome}/bin/sonar-scanner"
						}
						break
					default:
						echo "Sonar not available for the build type ${buildtype}"
						return false
				}
			}
			else {
				echo "CODEQUAL is ${CODEQUAL}"
			}
		}

		 //Stage for quality gate status check
		 stage("Quality Gate"){
		 	if (CODEQUAL == "true") {
		 	  timeout(time: 1, unit: 'HOURS') {
				if (DEPLOY_OS == "windows"){
					bat "ping 127.0.0.1 -n 6 > nul"
				}
				else {
					sh "sleep 5"
				}
		 		def qg = waitForQualityGate()
		 		if (qg.status != 'OK') {
		 		  error "Pipeline aborted due to quality gate failure: ${qg.status}"
		 		}
		 		else{
		 			echo "QualityGate is PASSED"
		 		}
		 	  }
	 		}
			else {
				echo "CODEQUAL is ${CODEQUAL}"
			}
		}

		//Stage to compile the code
		stage('BuildType') {
		    def String buildtypeUpper = buildtype.toUpperCase()
			echo "${buildtypeUpper}"

			if (!env.UNITTEST){
				env.UNITTEST = 'true'
			}

			switch ("${buildtypeUpper}"){
				case "MAVEN":
					sh "rm -rf target/*"
					mvn_build "${appname}", "${branchname}"
					break
				case "ANGULAR":
					angularbuild_test "${appname}", "${branchname}"
					break
				case "ANT":
					sh "rm -rf target/*"
					antbuild "${appname}", "${branchname}", "${BUILDFILE}"
					echo "${artifactName}"
					break
				case "TAG":
					break
				default:
					echo 'buildtype missing/wrong in the Jenkinsfile'
					return false
			}
		}

		//If JUNITSTATE is not defined in any build types make it Pass as default
		if (!env.JUNITSTATE){
			env.JUNITSTATE = 'Pass'
		}

		if (JUNITSTATE == 'Fail'){
			echo 'JUnit failure - Please check the test results'
			return
		}

		//Stage to upload artifacts to artifactory
		stage ('upload') {
			if (buildtype != "tag"){
				//def server = Artifactory.server "SA_artifactory"
				def server = Artifactory.server "StateAuto_Artifactory"
				def buildInfo = Artifactory.newBuildInfo()
				buildInfo.env.capture = true
				buildInfo.env.collect()

				def uploadSpec = """{
				"files": [
				  {
					"pattern": "target/*.war",
					"target": "StateAuto-Artifacts/${appname}/${branchname}/"
				  },
				  {
					"pattern": "target/*.zip",
					"target": "StateAuto-Artifacts/${appname}/${branchname}/"
				  }
				]
				}"""
				server.upload(uploadSpec)
				server.upload spec: uploadSpec, buildInfo: buildInfo
				server.publishBuildInfo buildInfo
			}
		}

		//Stage to auto deploy to L1
		stage('DeployL1'){
		        //echo "${L1env}"
			//check if auto deploy environment setup
			if (env.L1env) {
				def String deployenv = L1env.trim()
				def String envmain = deployenv.split('-')[0]

				if (envmain == "L1"){

					//setting the parameters for the Deployment pipeline
					def args = [string(name: 'environmentName', value: L1env),
								string(name: 'artifactName', value: artifactName)]

					if (env.AMIDeploy == "true"){

						// Run the Orchestrator job
						build job: 'Orchestrator/master', parameters: args,
											propagate: true, wait: true
					}
					if (env.AppType == "Mule" || env.Deploy_Type == "DockerContainer"){
							def String L1jobName = "L1_${appname}_Deploy"
							echo "L1 job name is ${L1jobName}"
							build job: "${L1jobName}", parameters: args,
												propagate: true, wait: true
					}
					//if (env.Deploy_Type == "DockerContainer"){
						//	def String L1jobName = "L1_${appname}_Deploy"
						//	echo "L1 job name is ${L1jobName}"
						//	build job: "${L1jobName}", parameters: args,
						//						propagate: true, wait: true
					//}
					else {
						// Run the Deploy job
						build job: 'deploytoservers/Shared-lib_AutoDeploy', parameters: args,
											propagate: true, wait: true
					}
				}
				else{
					echo "Auto Deploy is set to ${envmain}. Auto deploy will only set to L1. Update the env.L1env in the Jenkinsfile"
					echo "Auto Deploy skipped"
					currentBuild.result = 'UNSTABLE'
				}
			}
			else {
				//No auto deploy required
				echo 'Auto Deloy not set'
			}
		}

		stage('NPM_test'){
			bat "ng test --watch false"

			//junit 'testresults/*xml'
			step([$class: 'JUnitResultArchiver',
				 testResults: 'testresults/*xml'])
		}


		return true
	}
	catch (e) {
		// If there was an exception thrown, the build failed
		currentBuild.result = "FAILED"
		throw e
	}
	finally {
		// Success or failure, always send notifications
		notifyBuild(currentBuild.result)
	}

}

def notifyBuild(String buildStatus = 'STARTED') {
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
	  /*def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
		<p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""*/
	  def details = """\n
    Job name - ${env.JOB_NAME}\n
    Build number - ${env.BUILD_NUMBER}\n
    Check console output at ${env.BUILD_URL}\n"""
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
