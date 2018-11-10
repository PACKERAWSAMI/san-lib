#!/usr/bin/env groovy

// Check if a stack exists
// Lookup is done by name
// Returns true/false
def call(stackName, region='us-east-1') {

    def command = "aws --region=${region} cloudformation describe-stacks --stack-name=${stackName}"
    def statusCode = sh script: command,
                        returnStatus: true

    def exists = false
    if(statusCode == 0) {
        exists = true
    }

    return exists

}
