#!/usr/bin/env groovy

// Checkout our CloudFormation repo
// This has the Deployer configurations in it as well to do deploys
def call(branch='develop',
         repo='ssh://git-codecommit.us-east-1.amazonaws.com/v1/repos/cloudformation',
         credentialsId='71002c86-a26b-4b5f-9e22-50b84eb01fb1') {

    git url: repo,
        credentialsId: credentialsId,
        branch: branch

}
