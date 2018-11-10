#!/usr/bin/env groovy

import hudson.model.AbstractProject
import hudson.tasks.Mailer
import hudson.model.User
import com.cloudbees.groovy.cps.NonCPS

def String [] divide
def String param
def String val
def int index
def String PrevBuild

//Shared library for updatedvt
def call(artifactName, reponame, deployenv) {

	//getting the appname from the artifactName
	def tempfile = 'new1.ini'
	def String appName = artifactName.split('_')[0]
	def String inifile="lastVersionDeployed-${appName}-${deployenv}-AWS.ini"
	def svnurl = "https://svnprod/svn/QA/SCDM/SCDMShares/E/SCDM-INI/Deploy/${reponame}"

	//stage('update_dvt'){

		def String svndir = "${reponame}/SVN_${BUILD_NUMBER}"
		sh "mkdir -p ${svndir}"
		//checkout the file
		sh "svn co ${svnurl} ${svndir} --depth empty"
		sh "cd ${svndir}; svn up ${inifile}"
		//sh "svn up ${inifile}"

		doDVT("${reponame}", "${artifactName}", "${inifile}", "${tempfile}")

		//update the file with tempfile
		sh "pwd"
		sh "rm -f ${reponame}/SVN_${BUILD_NUMBER}/${inifile}"
		sh "cd ${reponame}/SVN_${BUILD_NUMBER}; mv -f ${tempfile} ${inifile}"

		def svnmessage = "Updated ${inifile} after deploying ${artifactName}"

		sh "cd ${reponame}/SVN_${BUILD_NUMBER}; svn commit -m \"${svnmessage}\""

		sh "rm -rf ${reponame}/SVN_${BUILD_NUMBER}"

	//}
	return true
}

@NonCPS
def doDVT(reponame,artifactName, inifile, tempfile ) {
		def file1 = new File("${workspace}/${reponame}/SVN_${BUILD_NUMBER}/${inifile}")
		def file = new File("${workspace}/${reponame}/SVN_${BUILD_NUMBER}/${tempfile}")
		def lines = file1.readLines()
		def linesize = lines.size()
		def datetime = new Date()
		def dtstamp = datetime.format("MM/dd/yyyy HH:mm:ss.SSS")


		//find the email

			def String email = 'anonymus'
			def id = getBuildUser()
			if (id == 0){
				email = 'Triggered by Auto Deploy'
			}
			else {
				User u = User.get(id)
				def umail = u.getProperty(Mailer.UserProperty.class)
				email=umail.getAddress()
			}

		//Parse line by line
		for (index=0; index < linesize; index++){
			//parse the line
			divide=lines[index].split("=")
			param=divide[0]
			if(divide.length > 1){
				val=divide[1]
			}
			else {
				val='null'
			}
			if (param == 'AUTOMATION_TASK_STARTEDBY'){
				replaceval= "${param}=${email}"
				lines[index]="${replaceval}"
			}

			if (param == 'BUILD_TO_DEPLOY'){
				PrevBuild = "${val}"
				replaceval= "${param}=${artifactName}"
				lines[index]="${replaceval}"
			}

			if (param == 'PREV_BUILD'){
				replaceval= "${param}=${PrevBuild}"
				lines[index]="${replaceval}"
			}

			if (param == 'DEPLOYMENT_STATUS'){
				dtstamp = datetime.format("MM/dd/yyyy HH:mm:ss.SSS")
				def String [] statusfull = val.split("<br>")
				def Statustime="${statusfull[0]}<br>${statusfull[1]}<br>[${dtstamp}]"
				replaceval= "${param}=${Statustime}"
				lines[index]="${replaceval}"
			}

			if (param == 'SCRIPT_START_TIME'){
				dtstamp = datetime.format("MM/dd/yyyy HH:mm:ss.SSS")
				replaceval= "${param}=[${dtstamp}]"
				lines[index]="${replaceval}"
			}

			if (param == 'SCRIPT_LOG'){
				//Split the job name to add a job in between
				echo "Job Name = ${JOB_NAME}"
				if (!JOB_NAME.contains("/")){
					consolelog = "http://l5-jenkins.ent.stateauto.com:8080/job/${JOB_NAME}/${BUILD_NUMBER}/console"
				}
				else{
					//Split the job name to add a job in between
					def String [] jobname = JOB_NAME.split("/")
					//find console log
					consolelog = "http://l5-jenkins.ent.stateauto.com:8080/job/${jobname[0]}/job/${jobname[1]}/${BUILD_NUMBER}/console"
				}

				replaceval= "${param}=${consolelog}"
				lines[index]="${replaceval}"
			}

			if (param == 'TIME_STAMP'){
				dtstamp = datetime.format("MM/dd/yyyy HH:mm:ss.SSS")
				replaceval= "${param}=[${dtstamp}]"
				lines[index]="${replaceval}"
			}

			file << ("${lines[index]}\r\n")
		}



}

@NonCPS
def getBuildUser() {
	def isStartedByUser = currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause)
	echo "UpStream: ${isStartedByUser}"
	if (isStartedByUser){
		 def build = currentBuild.rawBuild
		def upstreamCause
		while(upstreamCause = build.getCause(hudson.model.Cause$UpstreamCause)) {
			build = upstreamCause.upstreamRun
		}
		echo "Upstream: ${build}"
		if (!build.getCause(Cause.UserIdCause)){
			return 0
		}
		else  {
			return build.getCause(Cause.UserIdCause).getUserId()
		}
	}
	else {
		return currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
	}

}
