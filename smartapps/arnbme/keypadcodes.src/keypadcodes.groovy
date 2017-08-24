/**
 *  KeypadCodes
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
 */
definition(
    name: "KeypadCodes",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Use keypad pin codes to control system lighting and other things",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Keypad to monitor") {
        input "thekeypad", "capability.battery", required: true, title: "Keypad?"
        }
    section("Living Room Light") {
        input "theLRlight", "capability.switch", required: true, title: "Living Room Light?"
    	}
    section("Front Door Light") {
        input "theFDlight", "capability.switch", required: true, title: "Front Door Light?"
    	}
    section("Garage Door") {
        input "theGarageDoor", "capability.garageDoorControl", required: true, title: "Garage Door?"
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
    subscribe (thekeypad, "codeEntered", buttonHandler)
	}

def buttonHandler(evt)
	{
	log.debug "buttonHandler $evt $evt.value"
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	if (evt.value=="0000")
		{
		def status=theLRlight.currentState("switch").value
		if (status=="on")
			{
			theLRlight.off()
			}
		else
			{
			theLRlight.on()
			}
		}
	else
	if (evt.value=="1111")
		{
		def status=theFDlight.currentState("switch").value
		if (status=="on")
			{
			theFDlight.off()
			}
		else
			{
			theFDlight.on()
			}
		}
	else
	if (evt.value == "3333" && alarmstatus == "off")
		{
		theGarageDoor.open()
		}
	else
	if (evt.value=="4444")
		{
		theGarageDoor.close()
		}
	}