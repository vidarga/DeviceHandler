/**
 *  Copyright 2014 SmartThings
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
metadata {
	// Automatically generated. Make future change here.
	definition (name :"Virtual Temperature Dashboard", namespace: "vidarga", author: "vidarga") {
		capability "Temperature Measurement"
		capability "Sensor"
        
    	attribute "Temp1","number"
    	attribute "Temp2","number"
    	attribute "Temp3","number"

	command "changeTemp1", ["number"]
    	command "changeTemp2", ["number"]
    	command "changeTemp3", ["number"]
        command "setTemperature", ["number"]
	}

	preferences {
		input title: "Temperature Offset", description: "This feature allows you to correct any temperature variations by selecting an offset. Ex: If your sensor consistently reports a temp that's 5 degrees too warm, you'd enter \"-5\". If 3 degrees too cold, enter \"+3\".", displayDuringSetup: false, type: "paragraph", element: "paragraph"
		input "tempOffset", "number", title: "Degrees", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false
	} 
	// UI tile definitions
	tiles {
		valueTile("temperature", "device.temperature", inactiveLabel: false, width: 3, height: 3) {
            state("default", label:'${currentValue}°', decoration: "flat", unit:"C", canChangeIcon: true,
            		backgroundColors: getTempColors())
        }
                    
		valueTile("Temp1", "device.Temp1", inactiveLabel: false, width: 1, height: 1) {
            state("default", label:'${currentValue}°', unit:"C", canChangeIcon: true,
            		backgroundColors: getTempColors())                    
        }
        
        valueTile("Temp2", "device.Temp2", inactiveLabel: false, width: 1, height: 1) {
            state("default", label:'${currentValue}°', decoration: "flat", unit:"C", canChangeIcon: true,
            		backgroundColors: getTempColors())                    
        }

        valueTile("Temp3", "device.Temp3", inactiveLabel: false , width: 1, height: 1) {
            state("default", label:'${currentValue}°', decoration: "flat", unit:"C", canChangeIcon: true,
            		backgroundColors: getTempColors())                    
        }
        
		main "temperature"
		details(["temperature", "Temp1", "Temp2", "Temp3"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	def pair = description.split(":")
    def map = createEvent(name: pair[0].trim(), value: pair[1].trim(), unit:"C")
	def result = [map]
    return result
}

def setTemperature(value) {
	if (tempOffset) {
		def offset = tempOffset as int
		def v = value as int
		value = v + offset
	}
	sendEvent(name:"temperature", value: value, unit:"C")
}

def changeTemp1 (value) {
    sendEvent(name:"Temp1", value: value, unit:"C")
}

def changeTemp2 (value) {
    sendEvent(name:"Temp2", value: value, unit:"C")
}

def changeTemp3 (value) {
    sendEvent(name:"Temp3", value: value, unit:"C")
}


def getTempColors() {
	def colorMap
		colorMap = [
			// Celsius Color Range
			[value: 0, color: "#153591"],
			[value: 7, color: "#1e9cbb"],
			[value: 15, color: "#a9f9c5"],
			[value: 23, color: "#44b621"],
			[value: 29, color: "#f1d801"],
			[value: 33, color: "#d04e00"],
			[value: 36, color: "#bc2323"]
			]
}
  	
