# padoc-android

This is the ReadMe file for the Padoc library for Android

###TODO & BUGS

* Bluetooth paring dialog is not always dismissed, functionality is not compromised but it is still annoying.
* Create general functions that automate the multiple necessary steps to join a mesh, ~~or even to start scanning properly~~, etc.
* Integrate peer names and hops and make them available.
* Detect and implement deconnections!
* Implement priority peers/leaf nodes.
* Detect when bluetooth pairing/connection did not work. (out of reach, etc)
* ~~Sometimes the server socket is null and crashes (listenusingrfcommwithservicerecord returns null)~~ OK
* ~~Implement connection when the other device is already paired~~ OK
* ~~Try connections without bluetooth discovery~~ OK
* ~~Turn Wifi on automatically~~ OK
* ~~Reset Wifi at start~~
* ~~Reset Bluetooth at start~~
* ~~Get Bluetooth and Wifi status when starting~~
* Remove time limit on Wifi-Direct discovery
* Include mesh info in advertised service
* __Stop service advertising when killing app__
* Re-register service every so often. Otherwise if device is not scanning it won't be discovered. Weird.
* __Don't try to connect if another connection is being made__
