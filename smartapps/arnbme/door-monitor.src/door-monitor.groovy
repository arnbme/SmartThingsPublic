/**
 *  Door Monitor
 *	Issue Warning when door that is not monitored by Smarthome remains open when alarm is set to armed
 *
 *  Copyright 2017 Arn Burkhoff
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Aug 08, 2017 v2.0.0a add routine name to unschedule or it kills everything
 *	Aug 08, 2017 v2.0.0  Add subscription to location alarm state and logic to handle it
 *					define and use standard killit and new_monitor routines
 *					remove uneeded timimg stuff due to catching alarm status 
 *					remove endless cycles when door was open and system unarmed
 *					unable to push out an error message to user at this time if no push or no sms
 *	Aug 07, 2017 v1.0.2  Due to reports of RunIn being unreliable, change to RunOnce
 *	Aug 05, 2017 v1.0.1b change seconds from 60 to thedelay*60-5 on first short delay eliminating a 5 second runIn
 *	Aug 03, 2017 v1.0.1a Remove extraneous unschedule() from contactOpenHandler.
 *	Aug 02, 2017 v1.0.1  Add logic in checkStatus ignoring instusions (handled by dooropens) as much as possible.
 *	Jul 31, 2017 v1.0.0  Coded and Installed
 *
 */
definition(
    name: "Door Monitor",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Warn when door than is not monitored by Smarthome remains open when alarm is armed",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Door Contact Sensor to Monitor:") {
        input "thedoor", "capability.contactSensor", required: true, title: "Where?"
    }
    section("Maximum number of warning messages") {
        input "maxcycles", "number", required: true, range: "1..99", default: "2", title: "Maximum Messages?"
    }
    section("Number of minutes between messages") {
        input "thedelay", "number", required: true, range: "1..15", default: "1"
    }

    section("Send Push Notification?") {
        input "thesendPush", "bool", required: false, default:false,
              title: "Send Push Notification when Opened?"
    }
    section("Send a text message to this number (optional)") {
        input "phone", "phone", required: false
    }

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() 
	{
//	if (phone || thesendPush)
//		{
		subscribe(thedoor, "contact.closed", contactClosedHandler)
	//	subscribe(thedoor, "contact.open", contactOpenHandler)
		subscribe(location, 'alarmSystemStatus', alarmStatusHandler)
//		}
//	else
//		{	     
//		log.error ("A push notification or sms message is required")
//		}
	}

	
def new_monitor()
	{
	log.debug "new_monitor called: cycles: $maxcycles"
	state.cycles = maxcycles
	def now = new Date()
	def runTime = new Date(now.getTime() + (thedelay * 60000))
	runOnce (runTime, checkStatus)
	}

def killit()
	{
	log.debug "killit called"
	state.remove('cycles')
	unschedule(checkStatus)	//kill any pending cycles
	}

//	This is generally when armed and not handled here, could ring a chime or something
//	commented out subsribe to contact open above to kill
def contactOpenHandler(evt)
	{
	def alarmstate = location.currentState("alarmSystemStatus")
	def alarmvalue = alarmstate.value
	log.debug "contactOpenHandler called: $evt.value alarm: $alarmvalue"
	if (alarmvalue == "stay" || alarmvalue == "away")
		{
		new_monitor()
		}
	}	

def contactClosedHandler(evt) {
	log.debug "contactClosedHandler called: $evt.value"
	killit()
	}

def alarmStatusHandler(evt)
	{
	log.debug("Door Monitor caught alarm status change: ${evt.value}")
	if (evt.value=="off")
		{
		killit()
		}
	else
		{
		def contactstate = thedoor.currentState("contact")
		def contactvalue = contactstate.value
		if (contactvalue != "open")		//we are done with this
			{
			killit()
			}
		else
			{
			new_monitor()
			}
		}
	}

def checkStatus()
	{
	// get the current state object for the contact sensor
	def contactstate = thedoor.currentState("contact")
	def contactvalue = contactstate.value

	// get the current state for alarm system
	def alarmstate = location.currentState("alarmSystemStatus")
	def alarmvalue = alarmstate.value
	log.debug "In checkStatus: Alarm: $alarmvalue Door: $contactvalue MessageCycles remaining: $state.cycles"

	// get time elapsed of the current alarm state
	def alarm_elapsedk = now() - alarmstate.date.time
	def alarm_elapsed = Math.round(alarm_elapsedk / 1000)	//round back to seconds
//	log.debug "Alarm $alarmvalue for $alarm_elapsed seconds"

	// get the time elapsed between now and when the door was set open
	def door_elapsedk = now() - contactstate.date.time
	def door_elapsed = Math.round(door_elapsedk / 1000)	//round back to seconds
//	log.debug "Door $contactvalue for $door_elapsed seconds"

//	calc standard next runOnce time
	def now = new Date()
	def runTime = new Date(now.getTime() + (thedelay * 60000))
	if (contactvalue == "open" && (alarmvalue == "stay" || alarmvalue == "away"))
		{
		state.cycles = state.cycles - 1
//			state.cycles--  note to self this does not work

//			issue the notification here as specified by user
		def message = "System is armed, but the ${thedoor.displayName} is open"
		if (thesendPush)
			{
			sendPush message
			}
		if (phone)
			{
			sendSms(phone, message)
			}
		if (thedelay>0 && state.cycles>0)
			{
			log.debug ("issued next checkStatus cycle $thedelay ${60*thedelay} seconds")
			runOnce(runTime,checkStatus)
			}
		}
	else
		{
		killit()
		}

	}