#!/usr/bin/env groovy

// Commits and pushes a branch
def call(branch, message, doPush=true, remote='origin',
         credentialsId='71002c86-a26b-4b5f-9e22-50b84eb01fb1') {

    // Add all changes to commit
    sh 'git add .'

    // Do the commit
    sh "git commit -am '${branch}: ${message}'"

    // Push branch to remote
    if(doPush) {
        sshagent(credentials: [credentialsId]) {
            sh "git push ${remote} ${branch}"
        }
    }

}
