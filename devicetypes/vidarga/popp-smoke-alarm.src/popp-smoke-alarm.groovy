/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *, runLocally: false, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false
 */
metadata {
	definition (name: "popp smoke alarm", namespace: "vidarga", author: "vidarga") {
		capability "Smoke Detector"
		capability "Sensor"
		capability "Battery"
		capability "Health Check"

		attribute "alarmState", "string"
		//zw:F type:0701 mfr:0154 prod:0100 model:0201 ver:2.01 zwv:4.05 lib:06 cc:5E,20,25,30,71,70,85,80,5A,59,73,86,72 ccOut:20 role:07 ff:8C01 ui:8C01
		//fingerprint mfr:"0154", prod:"0100", model:"0201", deviceJoinName: "POPP Smoke Detector and Alarm Sirene"
        /*
        COMMAND_CLASS_ZWAVEPLUS_INFO_V2 		0x5E
        COMMAND_CLASS_BASIC_V2 					0x20
        COMMAND_CLASS_SWITCH_BINARY_V2 			0x25
        
        Alarm	0x71
        */
        
        fingerprint type:"0701", mfr:"0154", prod:"0100", model:"0201", ver:"2.01", zwv:"4.05", lib:"06", cc:"5E,20,25,30,71,70,85,80,5A,59,73,86,72", ccOut:"20", role:"07", ff:"8C01", ui:"8C01", deviceJoinName: "POPP Smoke Detector and Alarm Sirene"
	}

	simulator {
		status "smoke": "command: 7105, payload: 01 FF"
		status "clear": "command: 7105, payload: 01 00"
		status "test": "command: 7105, payload: 0C FF"
		status "battery 100%": "command: 8003, payload: 64"
		status "battery 5%": "command: 8003, payload: 05"
	}

	tiles (scale: 2){
		multiAttributeTile(name:"smoke", type: "lighting", width: 6, height: 4){
			tileAttribute ("device.alarmState", key: "PRIMARY_CONTROL") {
				attributeState("clear", label:"clear", icon:"st.alarm.smoke.clear", backgroundColor:"#ffffff")
				attributeState("smoke", label:"SMOKE", icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13")
				attributeState("tested", label:"TEST", icon:"st.alarm.smoke.test", backgroundColor:"#e86d13")
			}
		}
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		main "smoke"
		details(["smoke", "battery"])
	}
}

def installed() {
// Device checks in every hour, this interval allows us to miss one check-in notification before marking offline
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

	def cmds = []
	createSmokeEvents("allClear", cmds) // allClear to set inital states for smoke and CO
	cmds.each { cmd -> sendEvent(cmd) }
}

def updated() {
// Device checks in every hour, this interval allows us to miss one check-in notification before marking offline
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def parse(String description) {
	def results = []
	if (description.startsWith("Err")) {
	    results << createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
		if (cmd) {
			zwaveEvent(cmd, results)
		}
	}
	log.debug "'$description' parsed to ${results.inspect()}"
	return results
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

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd, results) {
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

// SensorBinary and SensorAlarm aren't tested, but included to preemptively support future smoke alarms
//
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, results) {
	if (cmd.sensorType == physicalgraph.zwave.commandclasses.SensorBinaryV2.SENSOR_TYPE_SMOKE) {
		createSmokeEvents(cmd.sensorValue ? "smoke" : "smokeClear", results)
	} 
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd, results) {
	if (cmd.sensorType == 1) {
		createSmokeEvents(cmd.sensorState ? "smoke" : "smokeClear", results)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd, results) {
	results << createEvent(descriptionText: "$device.displayName woke up", isStateChange: false)
	if (!state.lastbatt || (now() - state.lastbatt) >= 56*60*60*1000) {
		results << response([
				zwave.batteryV1.batteryGet().format(),
				"delay 2000",
				zwave.wakeUpV1.wakeUpNoMoreInformation().format()
			])
	} else {
		results << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
	}
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, results) {
	def map = [ name: "battery", unit: "%", isStateChange: true ]
	state.lastbatt = now()
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName battery is low!"
	} else {
		map.value = cmd.batteryLevel
	}
	results << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, results) {
	//“Notification, CO2 Alarm” 0x71
    //Implements COMMAND_CLASS_WAKE_UP (0x84)
    //Implements COMMAND_CLASS_BATTERY (0x80)
	def encapsulatedCommand = cmd.encapsulatedCommand([ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
	state.sec = 1
	log.debug "encapsulated: ${encapsulatedCommand}"
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand, results)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		results << createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd, results) {
	def event = [ displayed: false ]
	event.linkText = device.label ?: device.name
	event.descriptionText = "$event.linkText: $cmd"
	results << createEvent(event)
}