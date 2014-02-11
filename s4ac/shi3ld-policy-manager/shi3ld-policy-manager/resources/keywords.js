//prefix is the turtle prefix associated to the vocabulary namespace
//localName used to compose the usri with the prefix ie prefix:locaName
//label is what is shown to the user (presentation) and is the val of rdfs:label prop taken from the vocabularies
keywords = {
	
	user:  [
		
		//rel -> relationship is a class not managed, participant not managed since has not a foaf:Person as domain
		
		{"uri": "http://purl.org/vocab/relationship/acquaintanceOf", "label": "Acquaintance Of", "prefix": "rel", "localName": "acquaintanceOf" },
		{"uri": "http://purl.org/vocab/relationship/ambivalentOf", "label": "Ambivalent Of", "prefix": "rel", "localName": "ambivalentOf"},
		{"uri": "http://purl.org/vocab/relationship/ancestorOf", "label": "Ancestor Of", "prefix": "rel", "localName": "ancestorOf"},
		{"uri": "http://purl.org/vocab/relationship/antagonistOf", "label": "Antagonist Of", "prefix": "rel", "localName": "antagonistOf"},
		{"uri": "http://purl.org/vocab/relationship/apprenticeTo", "label": "Apprentice To", "prefix": "rel", "localName": "apprenticeTo"},
		{"uri": "http://purl.org/vocab/relationship/childOf", "label": "Child Of", "prefix": "rel", "localName": "childOf"},
		{"uri": "http://purl.org/vocab/relationship/closeFriendOf", "label": "Close Friend Of", "prefix": "rel", "localName": "closeFriendOf"},
		{"uri": "http://purl.org/vocab/relationship/collaboratesWith", "label": "Collaborates With", "prefix": "rel", "localName": "collaboratesWith"},
		{"uri": "http://purl.org/vocab/relationship/collegueOf", "label": "Collegue Of", "prefix": "rel", "localName": "collegueOf"},
		{"uri": "http://purl.org/vocab/relationship/descendantOf", "label": "Descendant Of", "prefix": "rel", "localName": "descendantOf"},
		{"uri": "http://purl.org/vocab/relationship/employedBy", "label": "Employed By", "prefix": "rel", "localName": "employedBy"},
		{"uri": "http://purl.org/vocab/relationship/employerOf", "label": "Employer Of", "prefix": "rel", "localName": "employerOf"},
		{"uri": "http://purl.org/vocab/relationship/enemyOf", "label": "Enemy Of", "prefix": "rel", "localName": "enemyOf"},
		{"uri": "http://purl.org/vocab/relationship/engagedTo", "label": "Engaged To", "prefix": "rel", "localName": "engagedTo"},
		{"uri": "http://purl.org/vocab/relationship/friendOf", "label": "Friend Of", "prefix": "rel", "localName": "friendOf"},
		{"uri": "http://purl.org/vocab/relationship/grandchildOf", "label": "Grandchild Of", "prefix": "rel", "localName": "grandchildOf"},
		{"uri": "http://purl.org/vocab/relationship/grandparentOf", "label": "Grandparent Of", "prefix": "rel", "localName": "grandparentOf"},
		{"uri": "http://purl.org/vocab/relationship/hasMet", "label": "Has Met", "prefix": "rel", "localName": "hasMet"},
		{"uri": "http://purl.org/vocab/relationship/influencedBy", "label": "Influenced By", "prefix": "rel", "localName": "influencedBy"},
		{"uri": "http://purl.org/vocab/relationship/knowsByReputation", "label": "Knows By Reputation", "prefix": "rel", "localName": "knowsByReputation"},
		{"uri": "http://purl.org/vocab/relationship/knowsInPassing", "label": "Knows In Passing", "prefix": "rel", "localName": "knowsInPassing"},
		{"uri": "http://purl.org/vocab/relationship/knowsOf", "label": "Knows Of", "prefix": "rel", "localName": "knowsOf"},
		{"uri": "http://purl.org/vocab/relationship/lifePartnerOf", "label": "Life Partner Of", "prefix": "rel", "localName": "lifePartnerOf"},
		{"uri": "http://purl.org/vocab/relationship/livesWith", "label": "Lives With", "prefix": "rel", "localName": "livesWith"},
		{"uri": "http://purl.org/vocab/relationship/lostContactWith", "label": "Lost Contact With", "prefix": "rel", "localName": "lostContactWith"},
		{"uri": "http://purl.org/vocab/relationship/mentorOf", "label": "Mentor Of", "prefix": "rel", "localName": "mentorOf"},
		{"uri": "http://purl.org/vocab/relationship/neighborOf", "label": "Neighbor Of", "prefix": "rel", "localName": "neighborOf"},
		{"uri": "http://purl.org/vocab/relationship/parentOf", "label": "Parent Of", "prefix": "rel", "localName": "parentOf"},
		{"uri": "http://purl.org/vocab/relationship/participantIn", "label": "Participant In", "prefix": "rel", "localName": "participantIn"},
		{"uri": "http://purl.org/vocab/relationship/siblingOf", "label": "Sibling Of", "prefix": "rel", "localName": "siblingOf"},
		{"uri": "http://purl.org/vocab/relationship/spouseOf", "label": "Spouse Of", "prefix": "rel", "localName": "spouseOf"},
		{"uri": "http://purl.org/vocab/relationship/worksWith", "label": "Works With", "prefix": "rel", "localName": "worksWith"},
		{"uri": "http://purl.org/vocab/relationship/wouldLikeToKnow", "label": "Would Like To Know", "prefix": "rel", "localName": "wouldLikeToKnow"},
		
		//foaf
		
		//domain = Person
		
		{"uri": "http://xmlns.com/foaf/0.1/geekcode", "label": "geekcode", "prefix": "foaf", "localName": "geekcode"},
		{"uri": "http://xmlns.com/foaf/0.1/firstName", "label": "firstName", "prefix": "foaf", "localName": "firstName"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/lastName", "label": "lastName", "prefix": "foaf", "localName": "lastName"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/surname", "label": "Surname", "prefix": "foaf", "localName": "Surname"},
		{"uri": "http://xmlns.com/foaf/0.1/family_name", "label": "family_name", "prefix": "foaf", "localName": "family_name"},
		{"uri": "http://xmlns.com/foaf/0.1/familyName", "label": "familyName", "prefix": "foaf", "localName": "familyName"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/plan", "label": "plan", "prefix": "foaf", "localName": "plan"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/img", "label": "image", "prefix": "foaf", "localName": "img"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/myersBriggs", "label": "myersBriggs", "prefix": "foaf", "localName": "plan"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/workplaceHomepage", "label": "workplace homepage", "prefix": "foaf", "localName": "workplaceHomepage"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/workInfoHomepage", "label": "work info homepage", "prefix": "foaf", "localName": "workInfoHomepage"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/schoolHomepage", "label": "schoolHomepage", "prefix": "foaf", "localName": "schoolHomepage"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/knows", "label": "knows", "prefix": "foaf", "localName": "knows"},
		{"uri": "http://xmlns.com/foaf/0.1/publications", "label": "publications", "prefix": "foaf", "localName": "publications"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/currentProject", "label": "current project", "prefix": "foaf", "localName": "currentProject"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/pastProject", "label": "past project", "prefix": "foaf", "localName": "pastProject"},//state testing
		{"uri": "http://xmlns.com/foaf/0.1/based_near", "label": "based near", "prefix": "foaf", "localName": "based_near"},
		
		
		//miscellaneous
		
		{"uri": "http://xmlns.com/foaf/0.1/weblog", "label": "weblog", "prefix": "foaf", "localName": "weblog"},//testing agent
		{"uri": "http://xmlns.com/foaf/0.1/icqChatID", "label": "ICQ chat ID", "prefix": "foaf", "localName": "icqChatID"},//agent testing subprop of nick
		{"uri": "http://xmlns.com/foaf/0.1/msnChatID", "label": "MSN chat ID", "prefix": "foaf", "localName": "msnChatID"},//agent testing subprop of nick
		{"uri": "http://xmlns.com/foaf/0.1/account", "label": "account", "prefix": "foaf", "localName": "account"},//state testing - domain Agent
		{"uri": "http://xmlns.com/foaf/0.1/age", "label": "age", "prefix": "foaf", "localName": "age"},//agent	unstable
		{"uri": "http://xmlns.com/foaf/0.1/mbox", "label": "personal mailbox", "prefix": "foaf", "localName": "mbox"},//agent
		{"uri": "http://xmlns.com/foaf/0.1/yahooChatID", "label": "Yahoo chat ID", "prefix": "foaf", "localName": "yahooChatID"},//agent testing subprop of nick
		{"uri": "http://xmlns.com/foaf/0.1/tipjar", "label": "tipjar", "prefix": "foaf", "localName": "tipjar"},//agent testing
		{"uri": "http://xmlns.com/foaf/0.1/jabberID", "label": "jabber ID", "prefix": "foaf", "localName": "jabberID"}, //agent testing subprop of nick
		{"uri": "http://xmlns.com/foaf/0.1/status", "label": "status", "prefix": "foaf", "localName": "status"},//agent unstable
		{"uri": "http://xmlns.com/foaf/0.1/openid", "label": "openid", "prefix": "foaf", "localName": "openid"},//agent testing
		{"uri": "http://xmlns.com/foaf/0.1/gender", "label": "gender", "prefix": "foaf", "localName": "gender"},//agent  tesing
		{"uri": "http://xmlns.com/foaf/0.1/interest", "label": "interest", "prefix": "foaf", "localName": "interest"},//agent testing
		{"uri": "http://xmlns.com/foaf/0.1/holdsAccount", "label": "account", "prefix": "foaf", "localName": "holdsAccount"},//same label of account prop (is not an error)
		{"uri": "http://xmlns.com/foaf/0.1/topic_interest", "label": "topic_interest", "prefix": "foaf", "localName": "topic_interest"},//agent testing
		{"uri": "http://xmlns.com/foaf/0.1/aimChatID", "label": "AIM chat ID", "prefix": "foaf", "localName": "aimChatID"},//agent subprop of nick testing
		{"uri": "http://xmlns.com/foaf/0.1/birthday", "label": "birthday", "prefix": "foaf", "localName": "birthday"},//agent unstable
		{"uri": "http://xmlns.com/foaf/0.1/made", "label": "made", "prefix": "foaf", "localName": "made"},//agent
		{"uri": "http://xmlns.com/foaf/0.1/skypeID", "label": "Skype ID", "prefix": "foaf", "localName": "skypeID"},//agent subprop of nick testing
		{"uri": "http://xmlns.com/foaf/0.1/mbox_sha1sum", "label": "sha1sum of a personal mailbox URI name", "prefix": "foaf", "localName": "mbox_sha1sum"},//testing agent
		{"uri": "http://xmlns.com/foaf/0.1/title", "label": "title", "prefix": "foaf", "localName": "title"},//testing no domain
		{"uri": "http://xmlns.com/foaf/0.1/nick", "label": "nickname", "prefix": "foaf", "localName": "nick"},//testing no domain 
		{"uri": "http://xmlns.com/foaf/0.1/dnaChecksum", "label": "DNA checksum", "prefix": "foaf", "localName": "nick"},//testing
		{"uri": "http://xmlns.com/foaf/0.1/givenName", "label": "Given name", "prefix": "foaf", "localName": "givenName"},//testing no domain
		{"uri": "http://xmlns.com/foaf/0.1/givenname", "label": "Given name", "prefix": "foaf", "localName": "givenname"},// no domain -same prop written in 2 diff way on foaf voc
		{"uri": "http://xmlns.com/foaf/0.1/phone", "label": "phone", "prefix": "foaf", "localName": "phone"},//testing no domain
		
		//prop of Thing check Spatial thing is subProp of Thing
		{"uri": "http://xmlns.com/foaf/0.1/name", "label": "name", "prefix": "foaf", "localName": "name"},
		{"uri": "http://xmlns.com/foaf/0.1/homepage", "label": "homepage", "prefix": "foaf", "localName": "homepage"},
		{"uri": "http://xmlns.com/foaf/0.1/maker", "label": "maker", "prefix": "foaf", "localName": "maker"},
		{"uri": "http://xmlns.com/foaf/0.1/depiction", "label": "depiction", "prefix": "foaf", "localName": "depiction"},//testing
		{"uri": "http://xmlns.com/foaf/0.1/fundedBy", "label": "funded by", "prefix": "foaf", "localName": "fundedBy"},
		{"uri": "http://xmlns.com/foaf/0.1/logo", "label": "logo", "prefix": "foaf", "localName": "logo"},//testing
		{"uri": "http://xmlns.com/foaf/0.1/theme", "label": "theme", "prefix": "foaf", "localName": "theme"}
	
	],
	
	dev: [
	
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#bluetoothName", "label": "Bluetooth Friendly Name", "prefix": "hard", "localName": "bluetoothName", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#bluetoothStatus", "label": "Bluetooth Status", "prefix": "hard", "localName": "bluetoothStatus", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#deviceHardware", "label": "Device Hardware", "prefix": "hard", "localName": "deviceHardware", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#imei", "label": "IMEI", "prefix": "hard", "localName": "imei", "prepend": "?dev"},//in dcn just tag present but no param (no type=property, no domain ecc), well spec in hard 
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#meid", "label": "MEID", "prefix": "hard", "localName": "meid", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#isTethered", "label": "Tethered", "prefix": "hard", "localName": "isTethered", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#deviceIdentifier", "label": "Device Identifier", "prefix": "hard", "localName": "deviceIdentifier", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/software.owl#deviceSoftware", "label": "Device Software", "prefix": "soft", "localName": "deviceSoftware", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/software.owl#characterColumns", "label": "Character Columns", "prefix": "soft", "localName": "characterColumns", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/software.owl#characterRows", "label": "Character Rows", "prefix": "soft", "localName": "characterRows", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/network.owl#networkSupport", "label": "Network Support", "prefix": "net", "localName": "networkSupport", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/java.owl#totalMemoryForJavaApps", "label": "JavaMaxMemorySize", "prefix": "java", "localName": "totalMemoryForJavaApps", "prepend": "?dev"},	
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model", "prefix": "common", "localName": "model", "prepend": "?dev"},//no domain
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version", "prefix": "common", "localName": "version", "prepend": "?dev"},//no domain
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor", "prefix": "common", "localName": "vendor", "prepend": "?dev"},//no domain
		
		//domain context entity (super class of device)
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id", "prefix": "common", "localName": "id", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name", "prefix": "common", "localName": "name", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#normativeStatus", "label": "Normative Status", "prefix": "common", "localName": "normativeStatus", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#restrictions", "label": "Restrictions", "prefix": "common", "localName": "restrictions", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#soundMode", "label": "Sound Mode", "prefix": "common", "localName": "soundMode", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#supports", "label": "Supports", "prefix": "common", "localName": "supports", "prepend": "?dev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#restrictions", "label": "Restrictions", "prefix": "common", "localName": "restrictions", "prepend": "?dev"},

        //common properties referred to hw -> devHw subclass of contextEntityHw subclass of ContextEntity ie all prop with domain contextEntity
        //It is chosen a subset of all property that could be applied to all subclasses of ContextEntity: only considered model, version, id, vendor, name
        
        //fictisious prop (not all prop of contextEntity)
        {"uri": "http://www.w3.org/2007/uwa/context/common.owl#", "label": "hardwareModel", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#", "label": "hardawareVersion", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#", "label": "hardwareVendor", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#", "label": "hardwareId", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#", "label": "hardwareName", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#battery", "label": "Battery", "prefix": "hard", "localName": "battery", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#bluetoothVersion", "label": "Supported Bluetooth Version", "prefix": "hard", "localName": "bluetoothVersion", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#builtInMemory", "label": "Built-in Memory", "prefix": "hard", "localName": "builtInMemory", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#display", "label": "Display", "prefix": "hard", "localName": "display", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#extensionMemory", "label": "Extension Memory", "prefix": "hard", "localName": "extensionMemory", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#hardwareComponent", "label": "Hardware Component", "prefix": "hard", "localName": "hardwareComponent", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#inputCharacterSets", "label": "Input Character Sets", "prefix": "hard", "localName": "inputCharacterSets", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#inputDevice", "label": "Input Device", "prefix": "hard", "localName": "inputDevice", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#memory", "label": "Memory", "prefix": "hard", "localName": "memory", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#outputCharacterSets", "label": "Output Character Sets", "prefix": "hard", "localName": "outputCharacterSets", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#outputDevice", "label": "Output Device", "prefix": "hard", "localName": "outputDevice", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#pointingResolution", "label": "Pointing Resolution", "prefix": "hard", "localName": "pointingResolution", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#primaryCamera", "label": "Primary Camera", "prefix": "hard", "localName": "primaryCamera", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#secondaryCamera", "label": "Secondary Camera", "prefix": "hard", "localName": "secondaryCamera", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		{"uri": "http://www.w3.org/2007/uwa/context/hardware.owl#softkeyNumber", "label": "Softkey Number", "prefix": "hard", "localName": "softkeyNumber", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw"},
		
		//common properties referred to hw properties
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Battery)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:battery ?bat.\n?bat a hard:Battery.\n?bat"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Battery)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:battery ?bat.\n?bat a hard:Battery.\n?bat"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Battery)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:battery ?bat.\n?bat a hard:Battery.\n?bat"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Battery)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:battery ?bat.\n?bat a hard:Battery.\n?bat"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Battery)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:battery ?bat.\n?bat a hard:Battery.\n?bat"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Built In Memory)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:builtInMemory ?bimm.\n?bimm a hard:MemoryUnit.\n?bimm"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Built In Memory)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:builtInMemory ?bimm.\n?bimm a hard:MemoryUnit.\n?bimm"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Built In Memory)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:builtInMemory ?bimm.\n?bimm a hard:MemoryUnit.\n?bimm"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Built In Memory)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:builtInMemory ?bimm.\n?bimm a hard:MemoryUnit.\n?bimm"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Built In Memory)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:builtInMemory ?bimm.\n?bimm a hard:MemoryUnit.\n?bimm"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Display)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:display ?dis.\n?dis a hard:Display.\n?dis"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Display)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:display ?dis.\n?dis a hard:Display.\n?dis"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Display)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:display ?dis.\n?dis a hard:Display.\n?dis"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Display)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:display ?dis.\n?dis a hard:Display.\n?dis"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Display)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:display ?dis.\n?dis a hard:Display.\n?dis"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Extension Memory)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:extensionMemory ?em.\n?em a hard:MemoryUnit.\n?em"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:extensionMemory ?em.\n?em a hard:MemoryUnit.\n?em"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Extension Memory)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:extensionMemory ?em.\n?em a hard:MemoryUnit.\n?em"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "extensionMemoryId (of Extension Memory)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:extensionMemory ?em.\n?em a hard:MemoryUnit.\n?em"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Extension Memory)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:extensionMemory ?em.\n?em a hard:MemoryUnit.\n?em"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Hardware Component)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:hardwareComponent?hwcom.\n?hwcom a hard:HardwareComponent.\n?hwcom"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Hardware Component)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:hardwareComponent?hwcom.\n?hwcom a hard:HardwareComponent.\n?hwcom"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Hardware Component)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:hardwareComponent?hwcom.\n?hwcom a hard:HardwareComponent.\n?hwcom"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "hardwareComponentId (of Hardware Component)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:hardwareComponent?hwcom.\n?hwcom a hard:HardwareComponent.\n?hwcom"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Hardware Component)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:hardwareComponent?hwcom.\n?hwcom a hard:HardwareComponent.\n?hwcom"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Input Device)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:inputDevice ?indev.\n?indev a hard:InputDevice.\n?devin"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Input Device)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:inputDevice ?indev.\n?indev a hard:InputDevice.\n?devin"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Input Device)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:inputDevice ?indev.\n?indev a hard:InputDevice.\n?devin"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Input Device)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:inputDevice ?indev.\n?indev a hard:InputDevice.\n?devin"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Input Device)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:inputDevice ?indev.\n?indev a hard:InputDevice.\n?devin"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Memory)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:memory ?mem.\n?mem a hard:MemoryUnit.\n?mem"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Memory)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:memory ?mem.\n?mem a hard:MemoryUnit.\n?mem"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Memory)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:memory ?mem.\n?mem a hard:MemoryUnit.\n?mem"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Memory)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:memory ?mem.\n?mem a hard:MemoryUnit.\n?mem"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Memory)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:memory ?mem.\n?mem a hard:MemoryUnit.\n?mem"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Output Device)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:outputDevice ?outdev.\n?outdev a hard:OutputDevice.\n?outdev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Output Device)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:outputDevice ?outdev.\n?outdev a hard:OutputDevice.\n?outdev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Output Device)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:outputDevice ?outdev.\n?outdev a hard:OutputDevice.\n?outdev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Output Device)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:outputDevice ?outdev.\n?outdev a hard:OutputDevice.\n?outdev"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Output Device)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:outputDevice ?outdev.\n?outdev a hard:OutputDevice.\n?outdev"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Primary Camera)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:primaryCamera ?primcam.\n?primcam a hard:Camera.\n?primcam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Primary Camera)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:primaryCamera ?primcam.\n?primcam a hard:Camera.\n?primcam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Primary Camera)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:primaryCamera ?primcam.\n?primcam a hard:Camera.\n?primcam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Primary Camera)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:primaryCamera ?primcam.\n?primcam a hard:Camera.\n?primcam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Primary Camera)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:primaryCamera ?primcam.\n?primcam a hard:Camera.\n?primcam"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Secondary Camera)", "prefix": "common", "localName": "model", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:secondaryCamera ?seccam.\n?seccam a hard:Camera.\n?seccam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Secondary Camera)", "prefix": "common", "localName": "version", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:secondaryCamera ?seccam.\n?seccam a hard:Camera.\n?seccam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Secondary Camera)", "prefix": "common", "localName": "vendor", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:secondaryCamera ?seccam.\n?seccam a hard:Camera.\n?seccam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Secondary Camera)", "prefix": "common", "localName": "id", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:secondaryCamera ?seccam.\n?seccam a hard:Camera.\n?seccam"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Secondary Camera)", "prefix": "common", "localName": "name", "prepend": "?dev hard:deviceHardware ?devhw.\n?devhw a hard:DeviceHardware.\n?devhw hard:secondaryCamera ?seccam.\n?seccam a hard:Camera.\n?seccam"},
		
		//common properties referred to sw
        
        {"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Software)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Software)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Software)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Software)", "prefix": "common", "localName": "id", "prepend": "?dev soft:DeviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Software)", "prefix": "common", "localName": "name", "prepend": "?dev soft:DeviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/push.owl#applicationIds", "label": "Push Application Ids", "prefix": "push", "localName": "applicationIds", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/push.owl#defaultMmsClient", "label": "Default MMS Client", "prefix": "push", "localName": "defaultMmsClient", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/push.owl#defaultWapPushClient", "label": "Default WAP Push Client", "prefix": "push", "localName": "defaultWapPushClient", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/web.owl#defaultWebBrowser", "label": "Default Web Browser", "prefix": "web", "localName": "defaultWebBrowser", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/web.owl#availableWREs", "label": "Available Web Runtime Environments", "prefix": "web", "localName": "availableWREs", "prepend": "?dev soft:DeviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/web.owl#defaultWRE", "label": "Default Web Runtime Environment", "prefix": "web", "localName": "defaultWRE", "prepend": "?dev soft:DeviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/software.owl#availableUserAgents", "label": "Available User Agents", "prefix": "soft", "localName": "availableUserAgents", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/software.owl#defaultHandler", "label": "Default Handler", "prefix": "soft", "localName": "defaultHandler", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/software.owl#operatingSystem", "label": "Active Operating System", "prefix": "soft", "localName": "operatingSystem", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/java.owl#availableJREs", "label": "Available Java Runtime Environments", "prefix": "java", "localName": "availableJREs", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        {"uri": "http://www.w3.org/2007/uwa/context/java.owl#defaultJRE", "label": "Default Java Runtime Environment", "prefix": "java", "localName": "defaultJRE", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw"},
        
        //common properties referred to sw properties
        
        {"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Default MMS Client)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultMmsClient ?mmscl.\n?mmscl a push:MmsClient.\n?mmscl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Default MMS Client)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultMmsClient ?mmscl.\n?mmscl a push:MmsClient.\n?mmscl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Default MMS Client)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultMmsClient ?mmscl.\n?mmscl a push:MmsClient.\n?mmscl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default MMS Client)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultMmsClient ?mmscl.\n?mmscl a push:MmsClient.\n?mmscl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default MMS Client)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultMmsClient ?mmscl.\n?mmscl a push:MmsClient.\n?mmscl"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Default WAP Push Client)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultWapPushClient ?wapcl.\n?wapcl a push:WapPushClient.\n?wapcl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Default WAP Push Client)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultWapPushClient ?wapcl.\n?wapcl a push:WapPushClient.\n?wapcl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Default WAP Push Client)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultWapPushClient ?wapcl.\n?wapcl a push:WapPushClient.\n?wapcl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default WAP Push Client)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultWapPushClient ?wapcl.\n?wapcl a push:WapPushClient.\n?wapcl"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default WAP Push Client)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw push:defaultWapPushClient ?wapcl.\n?wapcl a push:WapPushClient.\n?wapcl"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Default Web Browser)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWebBrowser ?wb.\n?wb a web:WebBrowser.\n?wb"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Default Web Browser)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWebBrowser ?wb.\n?wb a web:WebBrowser.\n?wb"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Default Web Browser)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWebBrowser ?wb.\n?wb a web:WebBrowser.\n?wb"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default Web Browser)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWebBrowser ?wb.\n?wb a web:WebBrowser.\n?wb"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default Web Browser)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWebBrowser ?wb.\n?wb a web:WebBrowser.\n?wb"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Default Web Runtime Environment)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWRE ?defwre.\n?defwre a web:WebRuntime.\n?defwre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Default Web Runtime Environment)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWRE ?defwre.\n?defwre a web:WebRuntime.\n?defwre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Default Web Runtime Environment)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWRE ?defwre.\n?defwre a web:WebRuntime.\n?defwre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default Web Runtime Environment)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWRE ?defwre.\n?defwre a web:WebRuntime.\n?defwre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default Web Runtime Environment)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:defaultWRE ?defwre.\n?defwre a web:WebRuntime.\n?defwre"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Available Web Runtime Environment)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:availableWREs ?wre.\n?wre a web:WebRuntime.\n?wre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Available Web Runtime Environment)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:availableWREs ?wre.\n?wre a web:WebRuntime.\n?wre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Available Web Runtime Environment)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:availableWREs ?wre.\n?wre a web:WebRuntime.\n?wre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Available Web Runtime Environment)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:availableWREs ?defwre.\n?wre a web:WebRuntime.\n?wre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Available Web Runtime Environment)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw web:availableWREs ?wre.\n?wre a web:WebRuntime.\n?wre"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Available User Agent)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:availableUserAgents ?ua.\n?ua a soft:UserAgent.\n?ua"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Available User Agent)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:availableUserAgents ?ua.\n?ua a soft:UserAgent.\n?ua"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Available User Agent)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:availableUserAgents ?ua.\n?ua a soft:UserAgent.\n?ua"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Available User Agent)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:availableUserAgents ?ua.\n?ua a soft:UserAgent.\n?ua"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Available User Agent)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:availableUserAgents ?ua.\n?ua a soft:UserAgent.\n?ua"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Default Handler)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:defaultHandler ?dh.\n?dh a soft:HandlingAssociation.\n?dh"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Default Handler)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:defaultHandler ?dh.\n?dh a soft:HandlingAssociation.\n?dh"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Default Handler)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:defaultHandler ?dh.\n?dh a soft:HandlingAssociation.\n?dh"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default Handler)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:defaultHandler ?dh.\n?dh a soft:HandlingAssociation.\n?dh"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default Handler)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:defaultHandler ?dh.\n?dh a soft:HandlingAssociation.\n?dh"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Active Operating System)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:operatingSystem ?os.\n?os a soft:OperatingSystem.\n?os"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Active Operating System)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:operatingSystem ?os.\n?os a soft:OperatingSystem.\n?os"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Active Operating System)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:operatingSystem ?os.\n?os a soft:OperatingSystem.\n?os"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Active Operating System)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:operatingSystem ?os.\n?os a soft:OperatingSystem.\n?os"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Active Operating System)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw soft:operatingSystem ?os.\n?os a soft:OperatingSystem.\n?os"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Default Java Runtime Environment)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:defaultJRE ?defjre.\n?defjre a java:JavaRuntimeEnvironment.\n?defjre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Default Java Runtime Environment)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:defaultJRE ?defjre.\n?defjre a java:JavaRuntimeEnvironment.\n?defjre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Default Java Runtime Environment)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:defaultJRE ?defjre.\n?defjre a java:JavaRuntimeEnvironment.\n?defjre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default Java Runtime Environment)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:defaultJRE ?defjre.\n?defjre a java:JavaRuntimeEnvironment.\n?defjre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default Java Runtime Environment)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:defaultJRE ?defjre.\n?defjre a java:JavaRuntimeEnvironment.\n?defjre"},
		
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#model", "label": "Model (of Available Java Runtime Environment)", "prefix": "common", "localName": "model", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:availableJREs ?jre.\n?jre a java:JavaRuntimeEnvironment.\n?jre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#version", "label": "Version (of Available Java Runtime Environment)", "prefix": "common", "localName": "version", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:availableJREs ?jre.\n?jre a java:JavaRuntimeEnvironment.\n?jre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#vendor", "label": "Vendor (of Available Java Runtime Environment)", "prefix": "common", "localName": "vendor", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:availableJREs ?jre.\n?jre a java:JavaRuntimeEnvironment.\n?jre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#id", "label": "Id (of Default Available Runtime Environment)", "prefix": "common", "localName": "id", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:availableJREs ?jre.\n?jre a java:JavaRuntimeEnvironment.\n?jre"},
		{"uri": "http://www.w3.org/2007/uwa/context/common.owl#name", "label": "Name (of Default Available Runtime Environment)", "prefix": "common", "localName": "name", "prepend": "?dev soft:deviceSoftware ?devsw.\n?devsw a soft:DeviceSoftware.\n?devsw java:availableJREs ?jre.\n?jre a java:JavaRuntimeEnvironment.\n?jre"}
		
    ],
    
    env: [
    	{"uri": "http://ns.inria.fr/prissma/v2#nearbyEntity", "label": "nearbyEntity", "prefix": "prissma", "localName": "nearbyEntity", "prepend": "?env"},
		{"uri": "http://ns.inria.fr/prissma/v2#motion", "label": "motion", "prefix": "prissma", "localName": "motion", "prepend": "?env"},
		{"uri": "http://purl.org/ontology/ao/core#activity", "label": "has activity", "prefix": "ao", "localName": "activity", "prepend": "?env"},
		{"uri": "http://ns.inria.fr/prissma/v2#poiLabel", "label": "poiLabel", "prefix": "prissma", "localName": "poiLabel", "prepend": "?env prissma:currentPOI ?poi.\n?poi a prissma:POI.\n?poi"},
		{"uri": "http://ns.inria.fr/prissma/v2#poiCategory", "label": "poiCategory", "prefix": "prissma", "localName": "poiCategory", "prepend": "?env prissma:currentPOI ?poi.\n?poi a prissma:POI.\n?poi"},
		{"uri": "http://xmlns.com/foaf/0.1/based_near", "label": "based near", "prefix": "foaf", "localName": "based_near", "prepend": "?env prissma:currentPOI ?poi.\n?poi a prissma:POI.\n?poi"}
    ]
}
