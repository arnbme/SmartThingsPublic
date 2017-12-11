/**
 *  Keypad_EntryDelay_Talker
 *  Supplements Big Talker adding speech when Keypad is set into Entry Delay Mode
 *		For LanNouncer Device: Chime, TTS text, Chime
 *		For speakers (such as Sonos)  TTS text
 *	Supports multiple keypads, LanNouncer devices and speakers
 *	When keypads use differant delay times, install multiple copies of this code
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
 *	Dec 08, 2017 Create 
 */
definition(
    name: "Keypad_EntryDelay_Talker",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Speak during Entry Delay. Used in conjunction with Big Talker or similar apps",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("The Entry Delay Message Settings") {
		input "theMsg", "string", required: true, title: "The message", 
			defaultValue: "Smart Home Monitor is arming in 30 seconds. Please exit the facility"
		input "thekeypads", "capability.button", required: true, multiple: true,
			title: "Keypads to monitor"
        input "theTTS", "capability.speechSynthesis", required: false, multiple: true,
        	title: "LanNouncer/DLNA TTS Devices"
        input "theSpeakers", "capability.audioNotification", required: false, multiple: true,
        	title: "Speaker Devices?"
		input "theVolume", "number", required: true, range: "1..100", defaultValue: 40,
			title: "Speaker Volume Level from 1 to 100"
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
	subscribe (thekeypads, "armMode", EntryDelayHandler)
	}


def EntryDelayHandler(evt)
	{
	log.debug("Keypad_EntryDelay_Talker entered, event: ${evt.value}")
	if (evt.value=="exitDelay")
		{
		if (theTTS)
			{
			theTTS.speak("@|ALARM=CHIME")
        	theTTS.speak(theMsg,[delay: 1800])
			theTTS.speak("@|ALARM=CHIME", [delay: 8000])
        	}
		if (theSpeakers)
			{
        	theSpeakers.playTextAndResume(theMsg,theVolume)
        	}
        }	
	}