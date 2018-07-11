/**
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
 *  Author: Vidarga
 */
metadata {
	definition(name: "popp-smoke-sirene", namespace: "vidarga", author: "vidarga", ocfDeviceType: "x.com.st.d.sensor.smoke", runLocally: true, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false) {
		capability "Smoke Detector" //attributes: smoke ("detected","clear","tested")
		capability "Actuator"
		capability "Alarm"
		capability "Battery"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Health Check"

		fingerprint type:"0701", mfr:"0154", prod:"0100", model:"0201", ver:"2.01", zwv:"4.05", lib:"06", cc:"5E,20,25,30,71,70,85,80,5A,59,73,86,72", ccOut:"20", role:"07", ff:"8C01", ui:"8C01", deviceJoinName: "POPP Smoke Detector and Alarm Sirene"

	}

	simulator {
		// reply messages
		reply "2001FF,2002": "command: 2003, payload: FF"
		reply "200100,2002": "command: 2003, payload: 00"
		reply "200121,2002": "command: 2003, payload: 21"
		reply "200142,2002": "command: 2003, payload: 42"
		reply "2001FF,delay 3000,200100,2002": "command: 2003, payload: 00"
		
		status "smoke": "command: 7105, payload: 01 FF"
		status "clear": "command: 7105, payload: 01 00"
		status "test": "command: 7105, payload: 0C FF"
	}

	tiles {
		multiAttributeTile(name:"smoke", type: "lighting", width: 6, height: 4){
			tileAttribute ("device.alarmState", key: "PRIMARY_CONTROL") {
				attributeState("clear", label:"clear", icon:"st.alarm.smoke.clear", backgroundColor:"#ffffff")
				attributeState("smoke", label:"SMOKE", icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13")
				attributeState("tested", label:"TEST", icon:"st.alarm.smoke.test", backgroundColor:"#e86d13")
			}
		}	
		standardTile("alarm", "device.alarm", width: 2, height: 2) {
			state "off", label: 'off', action: 'alarm.strobe', icon: "st.alarm.alarm.alarm", backgroundColor: "#ffffff"
			state "both", label: 'alarm!', action: 'alarm.off', icon: "st.alarm.alarm.alarm", backgroundColor: "#e86d13"
		}
		standardTile("off", "device.alarm", inactiveLabel: false, decoration: "flat") {
			state "default", label: '', action: "alarm.off", icon: "st.secondary.off"
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main "alarm"
		details(["smoke", "alarm", "off", "battery", "refresh"])
	}
}

def installed() {
	// Device-Watch simply pings if no device events received for 122min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, isStateChanged: true, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	initialize()
}

def updated() {
	// Device-Watch simply pings if no device events received for 122min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, isStateChanged: true, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

	runIn(12, "initialize", [overwrite: true, forceForLocallyExecuting: true])
}

def initialize() {
	def cmds = []

	// Set a limit to the number of times that we run so that we don't run forever and ever
	if (!state.initializeCount) {
		state.initializeCount = 1
	} else if (state.initializeCount <= 10) { // Keep checking for ~2 mins (10 * 12 sec intervals)
		state.initializeCount = state.initializeCount + 1
	} else {
		state.initializeCount = 0
		return // TODO: This might be a good opprotunity to mark the device unhealthy
	}

	if (!device.currentState("alarm")) {
		cmds << zwave.basicV1.basicGet().format()
		cmds << "delay 5"
	}
	
	if (!device.currentState("battery")) {
		if (zwaveInfo?.cc?.contains("80")) {
			cmds << zwave.batteryV1.batteryGet().format()
		} else {
			// Right now this DTH assumes all devices are battery powered, in the event a device is wall powered we should populate something
			sendEvent(name: "battery", value: 100, unit: "%")
		}
	}
	
	createSmokeEvents("allClear", cmds) // allClear to set inital states for smoke and CO
	
	if (cmds.size()) {
		sendHubCommand(cmds)

		runIn(12, "initialize", [overwrite: true, forceForLocallyExecuting: true])
	} else {
		state.initializeCount = 0
	}
}

def createSmokeEvents(name, results) {
	def text = null
	switch (name) {
		case "smoke":
			text = "$device.displayName smoke was detected!"
			// these are displayed:false because the composite event is the one we want to see in the app
			results << createEvent(name: "smoke",          value: "detected", descriptionText: text, displayed: false)
			break
		case "tested":
			text = "$device.displayName was tested"
			results << createEvent(name: "smoke",          value: "tested", descriptionText: text, displayed: false)
			break
		case "smokeClear":
			text = "$device.displayName smoke is clear"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text, displayed: false)
			name = "clear"
			break
		case "allClear":
			text = "$device.displayName all clear"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text, displayed: false)
			name = "clear"
			break
		case "testClear":
			text = "$device.displayName test cleared"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text, displayed: false)
			name = "clear"
			break
	}
	// This composite event is used for updating the tile
	results << createEvent(name: "alarmState", value: name, descriptionText: text)
}

def createEvents(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [name: "battery", unit: "%"]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery"
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbatt = new Date().time
	createEvent(map)
}

def poll() {
	if (secondsPast(state.lastbatt, 36 * 60 * 60)) {
		return zwave.batteryV1.batteryGet().format()
	} else {
		return null
	}
}

private Boolean secondsPast(timestamp, seconds) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.time
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().time - timestamp) > (seconds * 1000)
}

def on() {
	log.debug "sending on"
	[
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.basicV1.basicGet().format()
	]
}

def off() {
	log.debug "sending off"
	[
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.basicV1.basicGet().format()
	]
}

def strobe() {
	log.debug "sending stobe/on command"
	[
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.basicV1.basicGet().format()
	]
}

def both() {
	on()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.debug "ping() called"
	refresh()
}

def refresh() {
	log.debug "sending battery refresh command"
	[
		zwave.basicV1.basicGet().format(),
		zwave.batteryV1.batteryGet().format()
	]
}

def parse(String description) {
	log.debug "parse($description)"
	def result = null
	def cmd = zwave.parse(description, [0x20: 1, 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
	if (cmd) {
		result = createEvents(cmd)
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def createEvents(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	def switchValue = cmd.value ? "on" : "off"
	def alarmValue
	if (cmd.value == 0) {
		alarmValue = "off"
	} else if (cmd.value <= 33) {
		alarmValue = "strobe"
	} else if (cmd.value <= 66) {
		alarmValue = "siren"
	} else {
		alarmValue = "both"
	}
	[
		createEvent([name: "switch", value: switchValue, type: "digital", displayed: false]),
		createEvent([name: "alarm", value: alarmValue, type: "digital"])
	]
}

def createEvents(physicalgraph.zwave.Command cmd) {
	log.warn "UNEXPECTED COMMAND: $cmd"
}

def createEvents(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd, results) {
	if (cmd.zwaveAlarmType == physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_SMOKE) {
		if (cmd.zwaveAlarmEvent == 3) {
			createSmokeEvents("tested", results)
		} else {
			createSmokeEvents((cmd.zwaveAlarmEvent == 1 || cmd.zwaveAlarmEvent == 2) ? "smoke" : "smokeClear", results)
		}
	} else switch(cmd.alarmType) {
		case 1:
			createSmokeEvents(cmd.alarmLevel ? "smoke" : "smokeClear", results)
			break
		case 12:  // test button pressed
			createSmokeEvents(cmd.alarmLevel ? "tested" : "testClear", results)
			break
		case 13:  // sent every hour -- not sure what this means, just a wake up notification?
			if (cmd.alarmLevel == 255) {
				results << createEvent(descriptionText: "$device.displayName checked in", isStateChange: false)
			} else {
				results << createEvent(descriptionText: "$device.displayName code 13 is $cmd.alarmLevel", isStateChange:true, displayed:false)
			}
			
			// Clear smoke in case they pulled batteries and we missed the clear msg
			if(device.currentValue("smoke") != "clear") {
				createSmokeEvents("smokeClear", results)
			}
			
			// Check battery if we don't have a recent battery event
			if (!state.lastbatt || (now() - state.lastbatt) >= 48*60*60*1000) {
				results << response(zwave.batteryV1.batteryGet())
			}
			break
		default:
			results << createEvent(displayed: true, descriptionText: "Alarm $cmd.alarmType ${cmd.alarmLevel == 255 ? 'activated' : cmd.alarmLevel ?: 'deactivated'}".toString())
			break
	}
}
