#!/usr/bin/env groovy

// This will run Packer to build an AMI based on a Packer configuration file
// Required inputs: configFilePath, ami_version
// Option boolean input "validate" will optionally run Packer Validate before Build
// Additional optional inputs: awsKeyId, awsSecretKey
// Note: defaults to running /usr/local/bin/packer
//  but you can pass in packerBinary to specify a different path
// This step returns the AMI id created as a string
def call(Map inputs) {

    // This will be set later and returned
    def ami_id

    validateParameters inputs, ['configFilePath', 'ami_version']

    // Remove the file name from the string
    def packerDir = inputs.configFilePath.take(
                     inputs.configFilePath.lastIndexOf('/')
                    )    
    // Get only the file name from the string
    def configFile = inputs.configFilePath.drop(packerDir.size() + 1)

    // Default to this packer binary but let caller override if desired
    def packerBinary = '/usr/local/bin/packer'
    if(inputs.containsKey('packerBinary')) {
        packerBinary = inputs.packerBinary
    }

    dir(packerDir) {

        // Name the AMI based on branch
        def command = "-var ami_version=${inputs.ami_version}"

        // Add optional inputs as command line args
        if(inputs.containsKey('awsKeyId')) {
            command += " -var access_key=${inputs.awsKeyId}"
        }
        if(inputs.containsKey('awsSecretKey')) {
            command += " -var secret_key=${inputs.awsSecretKey}"
        }

        // Optionally run Packer Validate
        if(inputs.containsKey('validate') && inputs.validate) {
            sh "${packerBinary} validate ${configFile}"
        }

        def log_file = "${inputs.ami_version}.log"

        // Run the Build command and save the output so we can retrieve the AMI id
        sh "${packerBinary} build -machine-readable ${command} ${configFile} 2>&1 | tee ${log_file}"

        // Get the AMI id
        ami_id = sh script: "egrep -m1 -oe 'ami-.{8}' ${log_file}",
                    returnStdout: true

        // Remove the log file
        sh "rm ${log_file}"

    }

    return ami_id

}
