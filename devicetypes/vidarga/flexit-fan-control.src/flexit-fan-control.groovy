/**
 *  Virtual Device to control Multiple relays with webcore
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
 *
 */
 
metadata {
	definition (name: "flexit-fan-control", namespace: "vidarga", author: "vidarga") {
		capability "Switch Level"
		capability "Actuator"
		capability "Indicator"
		capability "Switch"
		capability "Sensor"

		command "lowSpeed"
		command "medSpeed"
		command "highSpeed"

		attribute "currentState", "string"

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
        
        preferences {
            input name:"hightimer", type:"number", title: "HIGH mode (in minutes)", description: "Adjust time duration in HIGH mode", range: "*..*", displayDuringSetup: false, defaultValue: "20"
        }        

		main(["switch"])
		details(["switch", "lowSpeed", "medSpeed", "highSpeed"])
	}
}


def parse(String description) {
}


def on() {
	sendEvent(name: "switch", value: "on", isStateChange: true)
    lowSpeed()
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true)
    sendEvent(name: "currentState", value: "OFF" as String)
}

def lowSpeed() {
    if (device.currentValue("switch") == "on") {
        sendEvent(name: "currentState", value: "LOW" as String)
        //log.debug "${device.displayName}: ${device.currentValue}"
    }
}

def medSpeed() {
    if (device.currentValue("switch") == "on") {
    	sendEvent(name: "currentState", value: "MED" as String)
        //log.debug "${device.displayName}: ${device.currentValue}"
    }
}

def highSpeed() {
	if (device.currentValue("switch") == "on") {
    	sendEvent(name: "currentState", value: "HIGH" as String)
        //log.debug "${device.displayName}: ${device.currentValue}"
    }
}
