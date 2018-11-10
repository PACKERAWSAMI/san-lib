#!/usr/bin/env groovy

//Shared library for emails
def call(templateflag, email_Sub, recipients) {

	stage("email") {

		switch ("${templateflag}"){
			case "MERGEERROR":
				emailext body: '''${SCRIPT, template="email-body-details-mergeError.html"}''',
					mimeType: 'text/html',
					subject: "${email_Sub}",
					to: "${recipients}"
				break
			case "MERGE":
				emailext body: '''${SCRIPT, template="email-body-info-merge.html"}''',
					mimeType: 'text/html',
					subject: "${email_Sub}",
					to: "${recipients}"
				break
			case "BUILD":
				emailext body: '''${SCRIPT, template="email-body-details-build.html"}''',
						mimeType: 'text/html',
						subject: "${email_Sub}",
						to: "${recipients}"
				break
			case "BUILDERROR":
				emailext body: '''${SCRIPT, template="email-body-details-buildError.html"}''',
						mimeType: 'text/html',
						subject: "${email_Sub}",
						to: "${recipients}"
				break
			case "DEPLOY":
				emailext body: '''${SCRIPT, template="email-body-details-deploy.html"}''',
						mimeType: 'text/html',
						subject: "${email_Sub}",
						to: "${recipients}"
				break
			case "DEPLOYERROR":
				emailext body: '''${SCRIPT, template="email-body-details-deployError.html"}''',
						mimeType: 'text/html',
						subject: "${email_Sub}",
						to: "${recipients}"
				break
		}
	}
	return true
}
