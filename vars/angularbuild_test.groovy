#!/usr/bin/env groovy

//This script supports only the Windows based build
//Shared library for Angular build
def call(appname, branchname) {

	def appfileversion = "${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}"
	def zipfiles = findFiles(glob: 'target\\*.zip/')
	boolean delcmdtargetstatus = zipfiles.length > 0
	echo "${delcmdtargetstatus}"
	env.NG_PATH = /C:\Users\svc_jenkins\AppData\Roaming\npm\ng/

	if (!env.RUN_NPM_INSTALL){
		env.RUN_NPM_INSTALL = 'true'
	}
	
	if (!env.ANGULAR_CMD){
		env.ANGULAR_CMD = 'true'
	}
	
	if (!env.DIR_TO_ZIP){
		env.DIR_TO_ZIP = 'dist'
	}
	
	//If NPM Install flag sets to true in Jenkinsfile, perform NPM_Install. 
	//NPM node modules will be part of source in old applications like SPA. So need to avoid node module install
	if (RUN_NPM_INSTALL == 'true'){
		stage('NPM_Install') {
			bat "npm install"
			
			if (UNITTEST == 'true'){
				bat "npm install karma-junit-reporter --save-dev"
			}
		}
	}
	
	//Compile angular code
	stage('Build') {
		bat "echo 'Angular setup'"
			
		if (ANGULAR_CMD == 'true'){
			bat "./build.bat"
		}
		else {
			def NG_COMMAND = /${NG_PATH} build --prod/
			bat "${NG_COMMAND}"
		}
	}
	
	if (UNITTEST == 'true'){
		//test angular code
		/*stage('NPM_test') {
			if (ANGULAR_CMD == 'true'){
				bat "ng test --watch false"
			}
			else{
				def NG_TEST_COMMAND = /${NG_PATH} test --watch false/
				bat "${NG_TEST_COMMAND}"
			}
			
			//junit 'testresults/*xml'
			step([$class: 'JUnitResultArchiver',
				 testResults: 'testresults/*xml'])
		}*/
	
		if (currentBuild.result != null) {
			env.JUNITSTATE = 'Fail'
			echo "JUNITSTATE: ${JUNITSTATE}"
			return
		}
		else {
			env.JUNITSTATE = 'Pass'
			echo "JUNITSTATE: ${JUNITSTATE}"
		}
	}
	else {
		env.JUNITSTATE = 'Pass'
		echo "JUNITSTATE: ${JUNITSTATE}"
	}
	
	//Compress the zip file from dist and copy to target
	stage('Compress'){
		def targetdir = fileExists "${workspace}/target"
		def zipfilepath = /"c:\Program Files\7-Zip\7z.exe"/ 
		def distzip = /.\${DIR_TO_ZIP}\*/
		def movecmdtarget = /target\./
		def delcmdtarget = /target\*.zip/
		//if (fileExists(delcmdtarget)) {
		if (delcmdtargetstatus){
			bat "del ${delcmdtarget}"
		
		}
		if (!targetdir){
			bat "mkdir target"
		}
		else {
			echo "${workspace}/target already available"
		}
		
		bat "${zipfilepath} a ${appfileversion}.zip ${distzip}"
		bat "move /y ${appfileversion}.zip ${movecmdtarget}"
		
		//assign the artifact to artifactName for deploy
		env.artifactName = "${appfileversion}.zip"

	}
	return true
}
