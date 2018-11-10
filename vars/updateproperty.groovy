#!/usr/bin/env groovy

def String envCurrent


//Shared library for build
def call(appName, artifact, serverproperties, deployenv) {
	
}


/*
=================================================================================
Description: Find the artifact in the lower environment from the property file
Parameter:
	deployenv - Lower Environment (Ex - L1-14)
	serverproperties - Path of the properties file


return - 
	LowerArtifact - On Success
	'Failure' - On Fail
==================================================================================
*/
def findArtifact(deployenv, serverproperties){
	def returnVal = sh (script: "cat ${serverproperties} | grep -i ${deployenv}.artifact= | grep -v ^#",returnStatus: true)
	
	if (!returnVal){
		def artifactentry = sh (script: "cat ${serverproperties} | grep -i ${deployenv}.artifact= | grep -v ^#",returnStdout: true)
		def String LowerArtifact = artifactentry.split('=')[1].trim()
		return "${LowerArtifact}"
		
	}
	else{
		return 'Failure'
	}
	
}

/*
=================================================================================
Description: Update the artifact in the property file
Parameter:
	artifact - Name of the artifact to be updated
	deployenv -  Environment (Ex - L1-14)
	serverproperties - Path of the properties file


return - 0 on Success
         non 0 on failure
	
==================================================================================
*/
def writeArtifact(artifact, deployenv, serverproperties){
	def returnstate
	def returnVal
	def int lineno
	def matchflag = 'false'
	//find the old artifact to replace
	def String toReplace = findArtifact(deployenv, serverproperties)
	def replacePattern = "${deployenv}.artifact=${toReplace}"
	def replacewith = "${deployenv}.artifact=${artifact}"
	def envcheck
	def int totallines
	
	echo "toReplace = ${toReplace}"
		
	if (toReplace != 'Failure'){
		returnVal = sh (script: "sed -i 's/${replacePattern}/${replacewith}/' ${serverproperties}",returnStatus: true)
		matchflag = 'true'
	}
	else {
		//if there is no entry in the properties file add it
		def int envsub = deployenv.split('-')[1] as int
		def envmain = deployenv.split('-')[0]
		envsub--
		
		echo "envsub = ${envsub}"
		
		//find the previous available environment artifact and write the content one line after 
		for (int i=envsub; i>=0; i--){
			envcheck = "${envmain}-${i}"
			
			echo "envcheck = ${envcheck}"
			
			returnVal = sh (script: "cat ${serverproperties} | grep -i ${envcheck}.artifact=",returnStatus: true)
			if (!returnVal){
				returnstate = sh (script: "cat ${serverproperties} | grep -n ${envcheck}.artifact=",returnStdout: true)
				lineno = returnstate.split(':')[0].trim() as int
				returnstate = sh(script:"wc -l ${serverproperties}", returnStdout: true)
				totallines = returnstate.split(" ")[0].trim() as int
				if (lineno != totallines){
					lineno++
					returnVal = sh (script: "sed -i '${lineno}i ${replacewith}' ${serverproperties}",returnStatus: true)
				}
				else {
					returnVal = sh (script: "echo ${replacewith} >> ${serverproperties}",returnStatus: true)
				}
				matchflag = 'true'
			}
			if (matchflag == 'true'){
				return
			}
		}
		
		if (matchflag == 'false'){
			//check for #L artifact and add it after that
			returnVal = sh (script: "cat ${serverproperties} | grep -i \'${envmain} artifacts\'",returnStatus: true)
			if (!returnVal){
				returnstate = sh (script: "cat ${serverproperties} | grep -n \'${envmain} artifacts\'",returnStdout: true)
				lineno = returnstate.split(':')[0].trim() as int
				returnstate = sh(script:"wc -l ${serverproperties}", returnStdout: true)
				totallines = returnstate.split(" ")[0].trim() as int
				
				
				if (lineno != totallines){
					lineno++
					returnVal = sh (script: "sed -i '${lineno}i ${replacewith}' ${serverproperties}",returnStatus: true)
				}
				else {
					returnVal = sh (script: "echo ${replacewith} >> ${serverproperties}",returnStatus: true)
				}
			}
			else {
				//if nothing just add the details in the end of the file
				def echomessage = "#${envmain} artifacts"
				echo "${echomessage}"
				sh "echo \"#${envmain} artifacts\" >> ${serverproperties}"
				returnVal = sh (script: "echo ${replacewith} >> ${serverproperties}",returnStatus: true)
				
			}
		}
	}
	
	return "${returnVal}"

}

/*
=================================================================================
Description: Upload the property file in artifactory
Parameter:
	appName - Application Name
	serverproperties - Path of the properties file


return - nothing
	
==================================================================================
*/
def uploadproperties(appName, serverproperties){

	//def server = Artifactory.server "SA_artifactory"
	def server = Artifactory.server "StateAuto_Artifactory"
	def buildInfo = Artifactory.newBuildInfo()
	buildInfo.env.capture = true
	buildInfo.env.collect()

	def uploadSpec = """{
	"files": [
	  {
		"pattern": "${serverproperties}",
		"target": "StateAuto-Artifacts/${appname}/"
	  }
	]
	}"""
	server.upload(uploadSpec)
	server.upload spec: uploadSpec, buildInfo: buildInfo
	server.publishBuildInfo buildInfo
	
	return

}
