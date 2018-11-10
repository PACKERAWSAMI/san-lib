#!/usr/bin/env groovy

// Run Deployer
// You need to pass in the configuration file you want to use (full path)
// It defaults to running an update but you pass in a different executeCommand such as 'create'
def call(configFile, executeCommand='update') {

    sh "deployer -c ${configFile} -s App -x ${executeCommand} -y"

}
