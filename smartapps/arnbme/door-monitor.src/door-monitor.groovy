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
        input "maxcycles", "number", required: true, title: "Maximum Messages?"
    }
    section("Number of minutes between messages") {
        input "thedelay", "number", required: true
    }

    section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
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

def initialize() {
	subscribe(thedoor, "contact.closed", contactClosedHandler)
	subscribe(thedoor, "contact.open", contactOpenHandler)
}

def contactOpenHandler(evt)
	{
	log.debug "contactOpenHandler called: $evt.value cycles: $maxcycles"
//	runIn (60*thedelay, checkStatus([data: [cycles: maxcycles]])) dont use runs it twice, first is immediate
	state.cycles = maxcycles
	runIn (60*thedelay, checkStatus)
	}

def contactClosedHandler(evt) {
	log.debug "contactClosedHandler called: $evt.value"
	state.remove('cycles')
	unschedule()	//kill any pending cycles
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

	if (contactvalue != "open")		//we are done with this
		{
		unschedule()				//just in case, but should not occur
		}
	else
	if (alarmvalue == "stay" || alarmvalue == "away")
		{
//		do nothing process intrusions
		if (state.cycles == maxcycles && maxcycles > 0 && thedelay > 0 && (thedelay * 60) < alarm_elapsed)
			{
			log.debug "check Status: Ignoring intrusion alert"
			}
		else
		if (door_elapsed < (thedelay * 60 - 5) || alarm_elapsed < (thedelay * 60 - 5))	//not worth the few second wait
			{
			log.debug ("waiting for delay to elapse before first message")
			if (door_elapsed < alarm_elapsed)
				runIn(thedelay * 60 - door_elapsed,checkStatus)
			else
				runIn(thedelay * 60 - alarm_elapsed,checkStatus)
			}
		else
			{
			state.cycles = state.cycles - 1
//			state.cycles--  note to self this does not work

//			issue the notification here as specified by user
			def message = "System is armed, but the ${thedoor.displayName} is open"
          		if (push)
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
				runIn(60 * thedelay,checkStatus)
				}
			}
		}
	else
//		door remains open with alarm status off. Must keep checking incase alarm is set without closing door
		{
//		log.debug ("issued next checkStatus with door open and alarm unarmed")
		runIn(60 * thedelay,checkStatus)
		}

	}