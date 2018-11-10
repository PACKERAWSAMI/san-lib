#!/usr/bin/env groovy

// Get the branch name from the artifact name
// Based off artifact naming convention of: appname_devbranchname_buildnumber_buildtimestamp.ext
def call(artifactName) {

    def branchName = artifactName.split('_')[1]

    return branchName

}
