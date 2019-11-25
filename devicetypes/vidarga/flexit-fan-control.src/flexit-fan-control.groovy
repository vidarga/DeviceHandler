/*  Virtual Device to control Multiple relays with webcore
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
 
metadata {
	definition (name: "flexit fan control", namespace: "vidarga", author: "vidarga",  executeCommandsLocally: false, mnmn: "SmartThings", vid: "generic-contact-2") {
		capability "Switch Level"
		capability "Actuator"
		capability "Indicator"
		capability "Switch"
		capability "Sensor"
        capability "Health Check"

		command "lowSpeed"
		command "medSpeed"
		command "highSpeed"
        command "offSpeed"
		command "humidity"

		attribute "currentState", "string"
        attribute "humidityCtrl", "string" 

	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true, canChangeBackground: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {    
				attributeState "on", action:"switch.off", label:'ON', icon:"st.Lighting.light24", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", action:"switch.on", label:'OFF', icon:"st.Lighting.light24", backgroundColor:"#ffffff", nextState:"turningOn"                
			}   
			tileAttribute ("device.currentState", key: "SECONDARY_CONTROL") {
           		attributeState "OFF", label:'Fan speed set to OFF', icon:"st.Lighting.light24"
                attributeState "LOW", label:'Fan speed set to LOW', icon:"st.Lighting.light24"
                attributeState "MED", label:'Fan speed set to MED', icon:"st.Lighting.light24"
                attributeState "HIGH", label:'Fan speed set to HIGH', icon:"st.Lighting.light24"
            }
		}
		standardTile("lowSpeed", "device.currentState", inactiveLabel: false, width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "default", label: 'LOW', action: "lowSpeed", icon:"st.Home.home30", backgroundColor: "#ffffff"
			state "LOW", label:'LOW', action: "lowSpeed", icon:"st.Home.home30", backgroundColor: "#00A0DC"
  		}
		standardTile("medSpeed", "device.currentState", inactiveLabel: false,  width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
        	state "default", label: 'MED', action: "medSpeed", icon:"st.Home.home30", backgroundColor: "#ffffff"
			state "MED", label: 'MED', action: "medSpeed", icon:"st.Home.home30", backgroundColor: "#79b821"
		}
		standardTile("highSpeed", "device.currentState", inactiveLabel: false,  width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
        	state "default", label: 'HIGH', action: "highSpeed", icon:"st.Home.home30", backgroundColor: "#ffffff"
			state "HIGH", label: 'HIGH', action: "highSpeed", icon:"st.Home.home30", backgroundColor: "#d60000"
		}
     
		standardTile("humidity", "device.humidityCtrl", inactiveLabel: false,  width: 6, height: 2, canChangeIcon: true, canChangeBackground: true)  {
        	state "on", label: 'on', action: "humidity", icon: "https://raw.githubusercontent.com/vidarga/MyDeviceHandler/master/images/Humidity.png", backgroundColor: "#00A0DC"
    		state "off", label: 'off', action: "humidity", icon: "https://raw.githubusercontent.com/vidarga/MyDeviceHandler/master/images/Humidity.png", backgroundColor: "#e2e2e2"
		}

		main(["switch"])
		details(["switch", "lowSpeed", "medSpeed", "highSpeed", "humidity"])
	}
}

def parse(String description) {
	/*def pair = description.split(":")
    def map = createEvent(name: pair[0].trim(), value: pair[1].trim())
	def result = [map]
    return result*/
}


def on() {
	sendEvent(name: "switch", value: "on", isStateChange: true)
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true)
    sendEvent(name: "currentState", value: "OFF" as String)
    state.humidityState="off"
    sendEvent(name:"humidityCtrl", value: "off")
}

def lowSpeed() {
    if (device.currentValue("switch") == "on") {
        sendEvent(name: "currentState", value: "LOW" as String)
    }
}

def medSpeed() {
    if (device.currentValue("switch") == "on") {
    	sendEvent(name: "currentState", value: "MED" as String)
    }
}

def offSpeed() {
	sendEvent(name: "currentState", value: "OFF" as String)
}

def highSpeed() {
	if (device.currentValue("switch") == "on") {
    	sendEvent(name: "currentState", value: "HIGH" as String)
    }
}

def humidity() {

       if(state.humidityState=="on"){
        	sendEvent(name:"humidityCtrl", value: "off")
            state.humidityState="off"
       }
       else {
        	sendEvent(name:"humidityCtrl", value: "on")
            state.humidityState="on"
       }
}

def installed() {
	log.trace "Executing 'installed'"
	initialize()
}

def updated() {
	log.trace "Executing 'updated'"
	initialize()
}

private initialize() {
	log.trace "Executing 'initialize'"

	sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
	sendEvent(name: "healthStatus", value: "online")
	sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
}

//log.debug "humidityCtrl state is $device.currentValue(humidityCtrl)"
//log.debug "humidityCtrl state is ${humidityCtrl}"
//log.debug "${device.displayName}: ${device.currentValue}"