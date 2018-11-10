#!/usr/bin/env groovy

// Checkout our Ansible repo
// This has the correct branch with the desired Packer package
def call(branch='develop',
         repo='ssh://git-codecommit.us-east-1.amazonaws.com/v1/repos/ansible',
         credentialsId='71002c86-a26b-4b5f-9e22-50b84eb01fb1') {

    git url: repo,
        credentialsId: credentialsId,
        branch: branch

}
