#!/usr/bin/env groovy

//Shared library for svn tag build
def call(appname, branchname) {

	stage('svntag') {

					//setting the svn path
					svnbranch = "${SVN_URL}"
					svntagpath = svnbranch.substring(0, svnbranch.lastIndexOf('/'))
					svntagpath = svntagpath.substring(0, svntagpath.lastIndexOf('/'))
					echo "${svntagpath}"
					//setting the artifact file name to ${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}
					def appfileversion = "${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}"
					svntag = "${svntagpath}/tags/${appfileversion}"
					svnmessage = "Automatically created tag ${svntag} from ${svnbranch}"
		if (DEPLOY_OS != "windows"){
					sh "svn copy --force-log --message \"${svnmessage}\" --non-interactive ${svnbranch} ${svntag}"
			}
		else {
			bat "svn copy --force-log --message \"${svnmessage}\" --non-interactive ${svnbranch} ${svntag}"
			}
		}
	return true
}
