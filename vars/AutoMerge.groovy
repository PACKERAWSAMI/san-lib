#!/usr/bin/env groovy

def MergeFromBranch
def MergeToBranch
def String[] Mergetobtanches
def String[] branches
def prgflag = 'false'


//Shared library for build
def call(appName) {
	env.defaultrecipients=''
	def String propertypath = "download/properties"
	def returnVal
	def automergeflag = 'true'
	def multimerge = 'false'
	def programname
	def program
	def branchnum

		//Checkout from version control
		stage ('Checkout') {
			//checkout scm
			if (CODEREPO=="git"){
				final scmVars = checkout(scm)
				//echo "scmVars: ${scmVars}"
				//echo "scmVars.GIT_BRANCH: ${scmVars.GIT_BRANCH}"
				MergeFromBranch = "${scmVars.GIT_BRANCH}"
				echo "branchname is: ${scmVars.GIT_BRANCH}"

				def String GIT_URL = "${scmVars.GIT_URL}"
				def String[] URL_git = GIT_URL.split('//')
				def String url_noHttps = URL_git[1]
				env.GITURL_nohttps = "${url_noHttps}"

			}
			if (CODEREPO=="svn"){
				def remote = scm.locations.first().remote
				def url = remote.split('@').first()
				env.SVN_URL = "${url}"
				echo "${SVN_URL}"
				def String[] branch = env.BRANCH_NAME.tokenize('/')
				def String branch_name = branch[1]
				MergeFromBranch = "${branch_name}"
				echo "MergeFromBranch = ${MergeFromBranch}"

			}
		}

		//Download from artifactory
		stage ('download') {
			//clean the server.peoperties
			sh "rm -rf ${propertypath}/server.properties"
			//sh "rm -rf ${artifactpath}/*"

			def server = Artifactory.server "StateAuto_Artifactory"
			def downloadSpec = """{
			"files": [
				{
					"pattern": "StateAuto-Artifacts/${appName}/server.properties",
					"target": "download/properties/"
				},
				{
					"pattern": "StateAuto-Artifacts/Systemfiles/*.properties",
					"target": "download/properties/"
				}
				]
			}"""

			server.download(downloadSpec)

			sh "ls -l ${propertypath}"
		}

		//initalize the variables
		stage('initialize') {
			//def MergeFromBranch = 'Crunch-2.5.0'
			def serverpath = "${propertypath}/${appName}"
			echo "Serverpath: ${serverpath}"


			def serverprop = readProperties  file: "${workspace}/${serverpath}/server.properties"
			//def program = serverprop['programname']
			env.EMAILRECIPIENTS = serverprop['RECIPIENTS']

			//find the program name from the project property file
			returnVal = sh (script: "cat ${workspace}/${serverpath}/server.properties | grep -i programname | grep -v ^#", returnStatus: true)
			echo "programname: ${programname}"
			//check the program name is empty
			if (returnVal){
				//Program name not defined in the project. properties file. So skip finding the merge to branch in the program files
				echo "======================================================================================="
				echo "Program Name not set in the ${appName}.properties file"
				echo "Checking in the automerge set for the ${MergeFromBranch} in the ${appName}.properties"
				echo "======================================================================================="
				prgflag = 'false'
			}
			else {
				programname = sh (script: "cat ${workspace}/${serverpath}/server.properties | grep -i programname | grep -v ^#", returnStdout: true)
				program = programname.split("=")[1].trim()
				prgflag = 'true'
			}

			if (prgflag == 'true') {
				//find the branch variable (branch0/branch1) for the Merge from branch
				returnVal = sh (script: "cat ${propertypath}/Systemfiles/${program}.properties | grep -i ${MergeFromBranch} | grep -v ^#", returnStatus: true)

				//if the branch is not set for automerge in the program.properties file
				if (returnVal){
					prgflag = 'false'
					return
				}

				def branchvarstring = sh (script: "cat ${propertypath}/Systemfiles/${program}.properties | grep -i ${MergeFromBranch} | grep -v ^#", returnStdout: true)

				def branchvar = branchvarstring.split("=")[0]

				echo "${branchvar}"

				//find the automerge branches for the merge from branch
				def String branchtovarstring = sh (script: "cat ${propertypath}/Systemfiles/${program}.properties | grep -i automerge.${branchvar} | grep -v ^#", returnStdout: true)

				//if the branch is not set for automerge in the program.properties file
				if (!branchtovarstring?.trim()){
					prgflag = 'false'
					return
				}

				def String branchtovar = branchtovarstring.split("=")[1].trim()

				echo "${branchtovar}"

				//find if there are multiple merge to branches
				branches = branchtovar.split(",")
				branchnum = branches.size()
				env.BRANCHSIZE="${branchnum}"

				def branchtovararray = new String[branchnum]

				echo "branches = ${branches}"

				//check if multi branch is set or not
				if (branchnum == 1){
					if (branchtovar == 'null'){
						automergeflag = 'false'
					}
					MergeToBranch = "${branchtovar}"
					multimerge = 'false'
				}
				else {
					multimerge = 'true'
				}

				if (automergeflag == 'false'){
					echo "Merge from ${MergeFromBranch} is disabled"
					env.MERGESTAT = 'False'
					return
				}

				def greptext
				for (int index = 0; index<branchnum; index++){
					//def branchtovararray1 = new String[branchnum]
					greptext = "${branches[index]}"
					branchtovarstring = sh (script: "cat ${propertypath}/Systemfiles/${program}.properties | grep -i ${greptext}= | grep -v automerge.${greptext}", returnStdout: true)
					branchtovar = branchtovarstring.split("=")[1].trim()
					branchtovararray[index] = "${branchtovar}" as String

					echo "output of grep = ${branchtovar}"
				}


				branches = branchtovararray
				echo "output of all grep = ${branchtovararray}"

			}

			//echo "${branchtovar}"
			//MergeToBranch = "${branchtovar}"

			//find the mergeto in the project.properties
			returnVal = sh (script: "cat ${serverpath}/server.properties | grep -i ${MergeFromBranch}= | grep -v ^#",returnStatus: true)

			echo "${returnVal}"

			if (!returnVal) {
				branchtovarstring = sh (script: "cat ${serverpath}/server.properties | grep -i ${MergeFromBranch}=",returnStdout: true)
				branchtovar = branchtovarstring.split("=")[1].trim()

				echo "${branchtovar}"
				//MergeToBranch = "${branchtovar}"

				returnVal = sh (script: "echo ${branchtovar} | grep ,",returnStatus: true)

				if (!returnVal){
					branches = branchtovar.split(",")
					echo " branchtovararray = ${branches}"
					multimerge = 'true'

				}
				else {
					if (branchtovar == 'null'){
						automergeflag = 'false'
					}
					MergeToBranch = "${branchtovar}"
				}


			}
			else {
				if (prgflag == 'false'){
					echo "No automerge setup for ${MergeFromBranch}"
					automergeflag = 'false'
				}
			}

			if (automergeflag == 'false'){
				echo "Merge from ${MergeFromBranch} is disabled"
				env.MERGESTAT = 'False'
				return
			}

		}

		if (env.MERGESTAT) {
			if (MERGESTAT == 'False'){
				return
			}
		}

		stage('merge'){
			if (multimerge == 'false'){
				echo "${MergeToBranch}"

				def dryrun = false
				def trunk = false

				//setting the parameters for the Deployment pipeline
				def args = [string(name: 'Application', value: appName),
								string(name: 'MergeFromBranch', value: MergeFromBranch),
								string(name: 'MergeToBranch', value: MergeToBranch),
								booleanParam(name: 'PerformDryRun', value: dryrun),
								booleanParam(name: 'MergetoTrunk', value: trunk)]

				if (CODEREPO=="svn"){

					// Run the Merge job
						//build job: 'Test_SVN_Merge/Shared_SVNMerge', parameters: args,
											//propagate: true, wait: true
						build job: 'SVN_merge/Shared-lib_AutoDeploy', parameters: args,
												propagate: true, wait: true

				}
				else {

					//Need to call Sri's git Merge job
					build job: 'GIT_merge/Shared-lib_AutoDeploy', parameters: args,
											propagate: true, wait: true
				}

			}
			else{
				echo "${branches}"
				// in this array we'll place the jobs that we wish to run
				def mergebranches = [:]
				def dryrun = false
				def trunk = false
				def mergeto
				def args

				branchnum = branches.size()

				echo "branchnum: ${branchnum}"

				for (int i = 0; i<branchnum; i++) {
					def ind = i //if we tried to use i below, it would equal 4 in each job execution.
					mergeto="${branches[i]}"
					echo "mergeto: ${mergeto}"

					//setting the parameters for the Deployment pipeline
					args = [string(name: 'Application', value: appName),
							string(name: 'MergeFromBranch', value: MergeFromBranch),
							string(name: 'MergeToBranch', value: mergeto),
							booleanParam(name: 'PerformDryRun', value: dryrun),
							booleanParam(name: 'MergetoTrunk', value: trunk)]

					if (CODEREPO=="svn"){
						//mergebranches["Mergebranch${i}"] = {
					try{
						//build job: 'Test_SVN_Merge/Shared_SVNMerge', parameters: args,
						//			propagate: false, wait: true

						def returnstatus = build(job: 'SVN_merge/Shared-lib_AutoDeploy', parameters: args, wait: true, propogate: false)
					}
					catch(err){
						echo "Auto merge failed for ${MergeFromBranch} to ${mergeto}. Please check the SVN_merge/Shared-lib_AutoDeploy job"
					}
						//}
					}
				}
				echo "${mergebranches}"
				//parallel mergebranches
			}
		}

		return true



}
