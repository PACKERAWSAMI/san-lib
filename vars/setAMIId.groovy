#!/usr/bin/env groovy

// Update a Deployer config file with a new API id
def call(configFileName, parameterName, amiId) {

    // Read in the YAML file
    configFile = readYaml file: configFileName

    // Overwrite the AMI id
    configFile.App.parameters[parameterName] = amiId

    // We need to remove the old config file before writing the new onw
    sh "rm ${configFileName}"

    // Write our updated YAML file to disk
    writeYaml file: configFileName,
              data: configFile

}
