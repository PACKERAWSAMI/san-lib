#!/usr/bin/env groovy
import hudson.model.AbstractProject
import hudson.tasks.Mailer
import hudson.model.User
import java.util.concurrent.TimeUnit
import com.cloudbees.groovy.cps.NonCPS

//Shared library for emails
def call() {

	def id = getBuildUser()
	echo " Userid: ${id}"
	
	return id
}

def getduration(){
	//def float duration = (currentBuild.getDuration()/1000)
	long millis = currentBuild.getDuration()
    long hours = TimeUnit.MILLISECONDS.toHours(millis)
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)

    String format = String.format("%02dhr:%02dmin:%02dsec", Math.abs(hours), Math.abs(minutes), Math.abs(seconds))
    
    return format
	
	//return duration;
}



def convert(int secondsToConvert) {
    long millis = secondsToConvert * 1000;
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1);

    String format = String.format("%02dhr:%02dmin:%02dsec", Math.abs(hours), Math.abs(minutes), Math.abs(seconds));
    
    return format
}

@NonCPS
def getBuildUser() {
	def isStartedByUser = currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause)
	echo "UpStream: ${isStartedByUser}"
	if (isStartedByUser){
		 def build = currentBuild.rawBuild
		def upstreamCause
		while(upstreamCause = build.getCause(hudson.model.Cause$UpstreamCause)) {
			build = upstreamCause.upstreamRun
		}
		echo "Upstream: ${build}"
		if (!build.getCause(Cause.UserIdCause)){
			return 0
		}
		else  {
			return build.getCause(Cause.UserIdCause).getUserId()
		}
	}
	else {
		return currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
	}

}
