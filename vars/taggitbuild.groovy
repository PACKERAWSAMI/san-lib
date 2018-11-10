#!/usr/bin/env groovy

//Shared library for svn tag build
def call(appname, branchname) {

	stage('gittag') {

				//setting the artifact file name to ${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}
				def appfileversion = "${appname}_${branchname}_${BUILD_NUMBER}_${BUILD_TIMESTAMP}"
				gitmessage = "Automatically created tag ${appfileversion} from ${branchname}"
				withCredentials([[$class: 'UsernamePasswordMultiBinding',
											credentialsId: "svc_jenkins",
											usernameVariable: 'GIT_USERNAME',
											passwordVariable: 'GIT_PASSWORD']]) {
		if (DEPLOY_OS != "windows"){
					sh "git tag -a ${appfileversion} -m 'Test_tag_${BUILD_NUMBER}'"
					sh "git push 'https://${GIT_USERNAME}:${GIT_PASSWORD}@${GITURL_nohttps}' --tags"
				}
		else {
			bat "git tag -a ${appfileversion} -m 'Test_tag_${BUILD_NUMBER}'"
			bat "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@${GITURL_nohttps} --tags"
		}
	 }
	}
	return true
}
