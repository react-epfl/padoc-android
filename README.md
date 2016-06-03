# padoc-android

This is the ReadMe file for the Padoc library for Android

###TODO & BUGS

* Bluetooth paring dialog is not always dismissed, functionality is not compromised but it is still annoying.
* Create general functions that automate the multiple necessary steps to join a mesh, ~~or even to start scanning properly~~, etc.
* ~~Integrate peer names and hops and make them available.~~
* Detect and implement deconnections!
* Implement priority peers/leaf nodes.
* Detect when bluetooth pairing/connection did not work. (out of reach, etc)
* ~~Sometimes the server socket is null and crashes (listenusingrfcommwithservicerecord returns null)~~ OK
* ~~Implement connection when the other device is already paired~~ OK
* ~~Try connections without bluetooth discovery~~ OK
* ~~Turn Wifi on automatically~~ OK
* ~~Reset Wifi at start~~
* ~~Reset Bluetooth at start~~
* ~~Get Bluetooth and Wifi status when starting~~ OK
* Remove time limit on Wifi-Direct discovery
* Implement mesh awarness
	* Include mesh info in advertised service
	* Send mesh info when connecting to server
* ~~__Stop service advertising when killing app__~~ 
* ~~Re-register service every so often. Otherwise if device is not scanning it won't be discovered. Weird.~~
	* Problem lies in discovery, wifi reset solves it, for now...
* ~~__Don't try to connect if another connection is being made__~~ OK
	* Pause server while attempting a connection
	* ~~Stop service while attempting a connection~~ OK
	* Detect or prevent double sockets
* ~~Clean discovery process~~ OK
* Make app work when in background mode
* ~~Send mesh peers to new peer in one message instead of many~~ OK
* Verify if source address is known when receiving a message
* Complete documentation
