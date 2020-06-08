/**
 *  DropboxWebhooks
 *
 *  Copyright 2020 Louis Mullineux
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
    name: "DropboxWebhooks",
    namespace: "lrmulli",
    author: "Louis Mullineux",
    description: "Dropbox Webhook Processor",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input "dbToken", "text", title: "Dropbox Token"
        
	}
    section ("Allow external service to control these things...") {
    input "switches", "capability.switch", multiple: true, required: true
  }
}

mappings {
  path("/webhook") {
    action: [
      GET: "webhook",
      POST: "webhookPost"
    ]
  }
}


def webhookPost() {
	log.debug "post received"
    def resp = []
    render contentType: "application/json", data: resp, status: 200
    def dateToday =  new Date().format( 'yyyyMMdd' ).toString()
    def folder = "/Apps/Swann Security/DVR8-4980/"+dateToday
    
    switches.each {
      getFileList(folder+"/"+it.displayName+"/picture",it)
    }

    return resp
}

def getFileList(String folder, sw){
    def dateToday =  new Date().format( 'yyyyMMdd' ).toString()
    //log.debug "date: $dateToday"
    def headers = [:]
    headers.put("Authorization", "Bearer "+dbToken)
    headers.put("Content-Type", "application/json")
    
    def params = [
    	uri: "https://api.dropboxapi.com/2/files/list_folder",
        headers: headers,
        body: '{\"path\": \"'+folder+'\",\"recursive\": true,\"include_media_info\": false,\"include_deleted\": false,\"include_has_explicit_shared_members\": false,\"include_mounted_folders\": true,\"include_non_downloadable_files\": true}'
		]
    try {
    	httpPost(params) { resp ->
        	resp.headers.each {
            	//log.debug "${it.name} : ${it.value}"
        	}
        def entryNames = resp.data.entries.findAll { it.name.contains(".jpg") }.name
        
        Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -2);
        
        def comparisonTime = cal.getTime();
        def newFile = false
        def tz = location.getTimeZone();
        
        for(item in entryNames){
    			def String[] str;
      			str = item.replaceAll(".jpg","").split('_')

				Date filedate = timeToday("20"+str[1]+"-"+str[2]+"-"+str[3]+"T"+str[4]+":"+str[5]+":"+str[6],tz);
                if(filedate.after(comparisonTime))
                {
                	newFile = true
                }
			}
            log.debug "New file found: $newFile"
            if(newFile){
				sw.on()
            }
    	}
	} catch (e) {
    	log.debug "something went wrong: $e"
        log.debug "params $params";
	}
        
}

def webhook() {
	def challenge = params.challenge
    log.debug "chalenge parameter: $challenge"
    def resp = challenge
    return resp
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
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers
