#!/usr/bin/env groovy

// Update a Deployer config file to make it ready to deploy to a specific environment
def call(configFileName, environment='L0', stackName='L0-App', release='develop') {

    // Read in the YAML file
    configFile = readYaml file: configFileName

    // Overwrite the environment properties
    configFile.App.release = release
    configFile.App.stack_name = stackName
    configFile.App.parameters.Environment = environment

    // We need to remove the old config file before writing the new onw
    sh "rm ${configFileName}"

    // Write our updated YAML file to disk
    writeYaml file: configFileName,
              data: configFile

}
