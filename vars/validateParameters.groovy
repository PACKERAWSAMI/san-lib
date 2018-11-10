#!/usr/bin/env groovy

// Validate that parameters aren't blank
// This step assumes parameters are strings
def call(parameters, parameterNames) {

    for(String parameterName: parameterNames) {

        if(!parameters[parameterName]) {

            error("Failed: Must provide a ${parameterName}")

        }

    }

}
