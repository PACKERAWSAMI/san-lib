#!/usr/bin/env groovy

// Checks out a branch
// If branch doesn't exist it creates it
def call(branch, remote='origin') {

    // Try to create a new branch
    def exists = sh script: "git checkout -b ${branch}",
                    returnStatus: true

    // If above command returned error code then branch must already exist
    // Use the existing branch and pull in any updates
    if(exists) {
        sh "git checkout ${branch}"
        sh "git pull ${remote} ${branch}"
    }

}
