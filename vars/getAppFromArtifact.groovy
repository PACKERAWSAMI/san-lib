#!/usr/bin/env groovy

// Get the application name from the artifact name
// Based off artifact naming convention of: appname_devbranchname_buildnumber_buildtimestamp.ext
def call(artifactName) {

    def appName = artifactName.split('_')[0]

    return appName

}
