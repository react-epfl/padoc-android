package com.react.gabriel.wbam.padoc;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Messenger;
import android.text.Html;
import android.text.Spanned;

import com.react.gabriel.wbam.R;
import com.react.gabriel.wbam.padoc.connection.BluetoothConnectedThread;
import com.react.gabriel.wbam.padoc.broadcastreceiver.BluetoothManager;
import com.react.gabriel.wbam.padoc.messaging.Message;
import com.react.gabriel.wbam.padoc.messaging.MessageManager;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;
import com.react.gabriel.wbam.padoc.routing.LeafRouter;
import com.react.gabriel.wbam.padoc.routing.ParentRouter;
import com.react.gabriel.wbam.padoc.routing.Router;
import com.react.gabriel.wbam.padoc.messaging.multicast.MulticastReceivedHandler;
import com.react.gabriel.wbam.padoc.messaging.multicast.MulticastReceivedListener;
import com.react.gabriel.wbam.padoc.messaging.multicast.MulticastReceiverService;
import com.react.gabriel.wbam.padoc.connection.WifiClientThread;
import com.react.gabriel.wbam.padoc.connection.WifiConnectedThread;
import com.react.gabriel.wbam.padoc.broadcastreceiver.GroupManager;
import com.react.gabriel.wbam.padoc.services.ServiceManager;
import com.react.gabriel.wbam.padoc.broadcastreceiver.WifiAdapter;

import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by gabriel on 27/06/16.
 */
public class Padoc implements MulticastReceivedListener, PadocAPI {

    private final UUID PADOC_UUID;

    private final int MAX_WIFI_CONNECTIONS;
    private final int MIN_BLUETOOTH_CONNECTIONS;
    private final int MAX_BLUETOOTH_CONNECTIONS;

    private boolean createCycles = true;

    public enum State{
        NULL,                     //Need to start everything.
        OFFLINE_SETTING_UP_SCAN,  //This is an intermediary state, wifi adapter is on but the peer is not currently active in any way.
        OFFLINE_SCANNING,         //This peer is currently scanning and looking to join a mesh.
        OFFLINE_WIFI_RESET_PRIOR_TO_CONNECTION,
        OFFLINE_CONNECTING,       //This peer is currently trying to connect to the mesh. Services are turned off.
        ONLINE_LEAF,              //This peer is in a Mesh as a client in a WiFi-Direct group. Services are turned off.
        OFFLINE_SETTING_UP_GO,    //This is a intermediary status, the peer is now GO but only Wifi is enabled. Services are turned off.
        OFFLINE_EXTENDING_MESH,
        ONLINE_GO,                //This peer is in a Mesh as a the GO of a WiFi-Direct group, accepting connections.
        ONLINE_GO_CONNECTING,     //This GO is either attempting a connection or receiving one. Services are turned off.
        ONLINE_GO_FULL,           //This peer is in a Mesh as a the GO of a WiFi-Direct group but it's not accepting any connections.
    }

    private State currentState;
    private State previousState;

    private boolean isParent;

    private final int randomValue;
    private String meshUUID;
    private final String localName;
    private final String localBluetoothMacAddress;

    private final Context context;
    private final Resources resources;
    private final PadocInterface padocInterface;

    private BluetoothManager bluetoothManager;
    private IntentFilter bluetoothManagerIntent;
    private final WifiAdapter wifiAdapter;
    private IntentFilter wifiAdapterIntent;
    private final GroupManager groupManager;
    private IntentFilter groupManagerIntent;
    private final ServiceManager serviceManager;

    private Router router;

    private MessageManager messenger;

    private ConnectedThread unidentifiedConnection;

    private String futureMeshUUID;
    private String futureBluetoothConnection;
    private String futureSSID;
    private String futurePassphrase;

    /**
     * Padoc : automatic multi-hop communication for Android
     * @param padocInterface Needs to extend Activity
     * @param context
     */
    public Padoc(PadocInterface padocInterface, Context context) {

        this.context = context;
        this.resources = context.getResources();
        this.padocInterface = padocInterface;

        this.MAX_WIFI_CONNECTIONS = this.resources.getInteger(R.integer.MAX_WIFI_CONNECTIONS);

        this.MIN_BLUETOOTH_CONNECTIONS = this.resources.getInteger(R.integer.MIN_BLUETOOTH_CONNECTIONS);
        this.MAX_BLUETOOTH_CONNECTIONS = this.resources.getInteger(R.integer.MAX_BLUETOOTH_CONNECTIONS);

        this.currentState = State.NULL;
        this.previousState = State.NULL;

        this.meshUUID = this.resources.getString(R.string.NULL_MESH_ID);
        this.PADOC_UUID = UUID.fromString(this.resources.getString(R.string.PADOC_UUID));
        this.randomValue = (int)(Math.random() * this.resources.getInteger(R.integer.MAX_RANDOM_VALUE));

        this.wifiAdapter = new WifiAdapter(this);
        this.wifiAdapterIntent = new IntentFilter();
        this.wifiAdapterIntent.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        this.context.registerReceiver(this.wifiAdapter, this.wifiAdapterIntent);

        this.groupManager = new GroupManager(this);
        this.groupManagerIntent = new IntentFilter();
        this.groupManagerIntent.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.groupManagerIntent.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.context.registerReceiver(this.groupManager, this.groupManagerIntent);

        this.serviceManager = new ServiceManager(this);

        this.bluetoothManager = new BluetoothManager(this);
        this.bluetoothManagerIntent = new IntentFilter();
        this.bluetoothManagerIntent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.bluetoothManagerIntent.addAction(BluetoothDevice.ACTION_FOUND);
        this.bluetoothManagerIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.bluetoothManagerIntent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.bluetoothManagerIntent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        this.context.registerReceiver(this.bluetoothManager, this.bluetoothManagerIntent);

        this.localName = this.bluetoothManager.getLocalName();
        this.localBluetoothMacAddress = this.bluetoothManager.getLocalAddress();

        //The background monitor that disables Wifi, Bluetooth and also erases any wifi network configuration when the app is killed
        Intent intent = new Intent(this.context, PadocMonitor.class);
        this.context.startService(intent);

        this.start();

    }

    //LIFECYCLE

    private void iterate(){

        switch (this.currentState){
            case NULL:

                if(wifiAdapter.getCurrentState().equals(WifiAdapter.State.STATE_NULL)){
                    print(Html.fromHtml("<h1><b><blue>Welcome</blue> <darkblue>to</darkblue> PadocChat proof of concept</h1></b>"), false);
                    this.wifiAdapter.enable();

                }else {
//                    print(Html.fromHtml("ERROR : unknown Padoc iteration #00"));
                }

                break;
            case OFFLINE_SETTING_UP_SCAN:

                if(this.previousState.equals(State.NULL)){
//                    print("WiFi enabled.");
                }

                this.serviceManager.start();

                break;
            case OFFLINE_SCANNING:

                if(serviceManager.getCurrentState().equals(ServiceManager.State.STATE_SERVICE_ADDED_DISCOVERY_RUNNING)){
//                    print("Padoc started, looking for peers...");
                }else {
//                    print("ERROR : unknown Padoc iteration #01");
                }

                break;
            case OFFLINE_CONNECTING:

                this.wifiAdapter.connectHack(this.futureSSID, this.futurePassphrase);
                this.futureSSID = null;
                this.futurePassphrase = null;
                //TODO : Start wifiConnectionTimeoutThread (it should call onWifiConnectionFailed())

                break;
            case OFFLINE_SETTING_UP_GO:

                if(this.groupManager.isGroupActive() && this.groupManager.isGroupOwner() && !this.groupManager.isServerRunning()){
                    //This group was just created, i.e. this peer just became GO
                    //isGroupOwner should always be true

//                    print(Html.fromHtml("We are now GO"), true);

                    //Set the meshUUID
                    this.meshUUID = UUID.randomUUID().toString();

                    this.setPeerAsParent();

                    //Start the wifi server
                    this.groupManager.startServer();

                    //Enable bluetooth adapter, start server and wait for notification
                    this.bluetoothManager.initialize();

                }else if(this.groupManager.isGroupActive() && this.groupManager.isGroupOwner() && this.groupManager.isServerRunning()){
                    //TODO : The p2pGroup changed. Check client list

                }else {
//                    print("ERROR : This should not happen");
                }

                break;
            case OFFLINE_EXTENDING_MESH:

                if(this.groupManager.isGroupActive() && this.groupManager.isGroupOwner() && !this.groupManager.isServerRunning()){

                    if(this.futureMeshUUID != null && this.futureBluetoothConnection != null){
                        this.meshUUID = this.futureMeshUUID;
                        this.futureMeshUUID = null;


                        this.setPeerAsParent();

                        //Start the wifi server
                        this.groupManager.startServer();

                        //Enable bluetooth adapter, start server and wait for notification
                        this.bluetoothManager.initialize();

                    }else{
                        print(Html.fromHtml("ERROR : futureMesh or futureBluetoothConnection is null when attempting mesh extension"), true);
                    }
                }else {
                    print(Html.fromHtml("ERROR : extending mesh error; Local group is not active or something"), true);
                }

                break;
            case ONLINE_GO_CONNECTING:

                this.serviceManager.stop(null);
                //wait for server connection (wifiSocketConnectionConfirmed)

                break;
            case ONLINE_LEAF:

                this.meshUUID = this.futureMeshUUID;
                this.futureMeshUUID = null;

                break;
            case ONLINE_GO:
                //Everything is in place

                this.serviceManager.start();

                break;
            case ONLINE_GO_FULL:
                //Nothing to do here.
                break;
        }
    }

    //NOTIFICATIONS

    public void onWifiAdapterEnabled(){
        if(this.currentState.equals(State.NULL)){
            this.setCurrentState(State.OFFLINE_SETTING_UP_SCAN);
            this.iterate();
        }else if(this.currentState.equals(State.OFFLINE_WIFI_RESET_PRIOR_TO_CONNECTION)){

            this.setCurrentState(State.OFFLINE_CONNECTING);
            this.iterate();
        }
    }

    public void onServiceAddedAndDiscoveryStarted(){
        if(this.currentState.equals(State.OFFLINE_SETTING_UP_SCAN)){
            this.setCurrentState(State.OFFLINE_SCANNING);
            this.iterate();
        }
    }

    public void onP2PGroupChanged(){
        if(this.currentState.equals(State.OFFLINE_SETTING_UP_GO) || this.currentState.equals(State.OFFLINE_EXTENDING_MESH)){
            this.iterate();
        }else if(this.currentState.equals(State.ONLINE_GO)){
            this.setCurrentState(State.ONLINE_GO_CONNECTING);
            this.iterate();
        }
    }

    public void onP2PGroupCreationFailed(){
        //TODO : Complete
        //reset router, currentState, etc
    }

    public void onBluetoothServerRunning(){
        if(this.currentState.equals(State.OFFLINE_SETTING_UP_GO)){
            this.setCurrentState(State.ONLINE_GO);
            this.iterate();
        }else if(this.currentState.equals(State.OFFLINE_EXTENDING_MESH)){

//            print("Attempting bluetooth mesh extension");
            this.setCurrentState(State.ONLINE_GO_CONNECTING);
            this.bluetoothManager.attemptConnectionWith(this.futureBluetoothConnection);
            this.futureBluetoothConnection = null;
        }

    }

    public void onWifiConnectionSucceeded(){

        this.setPeerAsLeaf();
    }

    public void onWifiConnectionFailed(){
        //TODO : complete
        this.setCurrentState(State.OFFLINE_SETTING_UP_SCAN);
        this.iterate();
    }

    public void onWifiSocketConnectionFailed(){
        //TODO : retry socket connection or close wifi connection

    }

    public void onBluetoothConnectionFailed(){

//        print("BLUETTOHT FAILED");

        if(this.currentState.equals(State.ONLINE_GO_CONNECTING)){
            this.setCurrentState(State.ONLINE_GO);
            this.iterate();
        }else{
//            print("ERROR : Bluetooth connection failed but we are not ONLINE_GO");
        }
    }

    public void onWifiSocketConnectionConfirmed(){

        if(this.currentState.equals(State.OFFLINE_CONNECTING)){
            this.setCurrentState(State.ONLINE_LEAF);
            this.iterate();
        }else if(this.currentState.equals(State.ONLINE_GO_CONNECTING)){
            this.setCurrentState(State.ONLINE_GO);
            this.iterate();
        }
    }
    //Should join these two methods
    public void onBluetoothSocketConnectionConfirmed(){
        if(this.currentState.equals(State.ONLINE_GO_CONNECTING) || this.currentState.equals(State.OFFLINE_EXTENDING_MESH)){

            if(this.router.getNumberOfBluetoothConnections() < MAX_BLUETOOTH_CONNECTIONS
                    || this.router.getNumberOfWifiConnections() < MAX_WIFI_CONNECTIONS){
                this.setCurrentState(State.ONLINE_GO);
            }else {
                this.setCurrentState(State.ONLINE_GO_FULL);
            }
            this.iterate();
        }else{
//            print("ERROR : currentState should be ONLINE_GO_CONNECTING or OFFLINE_EXTENDING_MESH");
        }
    }

    //Handlers

    public void handleWifiConnectedSocket(Socket wifiSocket){

//        print("Got WiFi Socket!");

        this.unidentifiedConnection = new WifiConnectedThread(this, wifiSocket);
        this.unidentifiedConnection.start();
        this.unidentifiedConnection.write(Message.getIDMessage(this.localBluetoothMacAddress, this.localName));

        //TODO : start ID_ACKTimoutThread; it should resend an IDMessage on timeout
    }

    public void handleBluetoothConnectedSocket(BluetoothSocket bluetoothSocket){
//        print ("Got Bluetooth Socket!");

        //We should send our ID and wait for ID_ACK
        this.unidentifiedConnection = new BluetoothConnectedThread(this, bluetoothSocket);
        this.unidentifiedConnection.start();
        this.unidentifiedConnection.write(Message.getIDMessage(this.localBluetoothMacAddress, this.localName));

        //TODO : start ID_ACKTimeoutThread; it should resend an IDMessage on timeout

        if(this.currentState.equals(State.ONLINE_GO_CONNECTING)){
            //This peer initiated the connection
//            print("this thing says I initiated the connection..");

        }else if(this.currentState.equals(State.ONLINE_GO)){
            //This peer is acting as the server
//            print("this thing says the other peer initiated the connection..");
            this.serviceManager.stop(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    setCurrentState(State.ONLINE_GO_CONNECTING);
                }

                @Override
                public void onFailure(int reason) {
                    print(Html.fromHtml("Error : Could not stop services"), true);
                }
            });
        }
    }

    public void handleNewPadocDiscovery(final String meshUUID, final int randomValue, final String ssid, final String pass, final String bluetoothMacAddress){
        //Only offline peers and parent nodes (GOs) are discoverable and scanning.

        switch (this.currentState){
            case OFFLINE_SCANNING:

                if(meshUUID.equals(this.resources.getString(R.string.NULL_MESH_ID))){
                    //Discovered peer is pretty lonely (OFFLINE_SCANNING)

                    if(this.randomValue >= randomValue){

                        //This peer should be GO
//                        print(Html.fromHtml("Preparing to be GO"), true);
                        this.serviceManager.stop(new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {

                                setCurrentState(State.OFFLINE_SETTING_UP_GO);
                                groupManager.createGroup();
                                //Wait for notification
                            }

                            @Override
                            public void onFailure(int reason) {
                                print(Html.fromHtml("Error : Could not stop services"), true);
                            }
                        });

                    }else{
                        //The discovered peer will be GO. Wait for discovery.
//                        print("Waiting for GO");
                    }
                }else {
                    //We just found a GO, check if he is accepting connections

                    if(ssid != null && pass != null){
                        //Discovered GO is accepting WiFi Connections

//                        print(Html.fromHtml("Found GO, attempting WiFi connection..."), true);

                        //Attempt connection and wait for notification
                        this.serviceManager.stop(new WifiP2pManager.ActionListener(){
                            public void onSuccess() {
                                futureSSID = ssid;
                                futurePassphrase = pass;
                                futureMeshUUID = meshUUID;
                                setCurrentState(State.OFFLINE_WIFI_RESET_PRIOR_TO_CONNECTION);
                                wifiAdapter.resetWiFi();
                            }

                            public void onFailure(int reason) {
                                print(Html.fromHtml("Error : Could not stop services"), true);
                            }
                        });


                    }else if(bluetoothMacAddress != null){
                        //Discovered GO is only Accepting Bluetooth Connections. We should be GO as well and extend this meshUUID through bluetooth

                        //TODO
//                        print("Preparing to be GO and extend existing meshUUID");
                        this.serviceManager.stop(new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                setCurrentState(State.OFFLINE_EXTENDING_MESH);
                                futureMeshUUID = meshUUID;
                                futureBluetoothConnection = bluetoothMacAddress;
                                groupManager.createGroup();
                                //Wait for notification
                            }

                            @Override
                            public void onFailure(int reason) {
                                print(Html.fromHtml("Error : Could not stop services"), true);
                            }
                        });


                    }else {
                        //Discovered GO is not accepting any type of Connections

                        //TODO : Should this peer even be advertising?
//                        print("Discovered GO is not accepting any kind of connection");
                    }
                }

                break;
            case ONLINE_GO:

//                print("We are GO");

                if(meshUUID.equals(this.meshUUID) && !this.router.isConnectedTo(bluetoothMacAddress) && this.randomValue > randomValue){
                    //Discovered peer is also a Parent Node and it is in the same mesh as us.

                    //Attempt connection and wait for notification
                    //TODO : Uncomment if you want to create cycles in the mesh
                    if(this.createCycles){
//                        print("Attempting cyclic bluetooth connection");
                        this.serviceManager.stop(new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                setCurrentState(State.ONLINE_GO_CONNECTING);
                                bluetoothManager.attemptConnectionWith(bluetoothMacAddress);
                            }

                            @Override
                            public void onFailure(int reason) {
                                print(Html.fromHtml("Error : Could not stop services"), true);
                            }
                        });
                    }

                }else if(!meshUUID.equals(this.resources.getString(R.string.NULL_MESH_ID)) && !meshUUID.equals(this.meshUUID)){

                    //TODO : We just discovered a parent node that is part of another mesh. We need to join meshes
//                    print("MESHES. WHAT DO.");
                }

                break;
        }
    }

    public void handleRawJsonMessage(ConnectedThread connectedThread, String jsonString){

//        print("Got message!");

        Message message = new Message();

        if(message.setMessage(jsonString)){
            if(message.getType().equals(Message.Type.ID)){

                if(connectedThread == this.unidentifiedConnection){

                    this.unidentifiedConnection.setRemoteAddress(message.getSource());
                    this.unidentifiedConnection.setRemoteName(message.getMsg());

                    this.router.handleNewConnection(this.unidentifiedConnection);

                    if(this.isParent){
//                        print("Forwarding new peer to mesh");
                        this.messenger.forwardNewPeerIDToMesh(this.unidentifiedConnection);
//                        print("SENDING KNOWNPEERS TO new peer");
                        this.messenger.sendMeshInfoTo(this.unidentifiedConnection.getRemoteAddress());
                    }

                    this.unidentifiedConnection = null;

                    if(connectedThread instanceof WifiConnectedThread){
//                        print("from WIFI");
                        this.onWifiSocketConnectionConfirmed();
                    }else if(connectedThread instanceof BluetoothConnectedThread){
//                        print("from BLUETOOTH");
                        this.onBluetoothSocketConnectionConfirmed();
                    }

                }else {
//                    print("ERROR : Got and ID_MESSAGE from an already identified thread");
                }

            }else {
                this.messenger.handleMessage(message, connectedThread);
            }
        }else {
//            print("ERROR : Message is corrupted!");
        }
    }

    //COMMANDS

    public void start(){
        this.iterate();
    }

    public void stop(){
        //TODO : COMPLETE
        this.context.unregisterReceiver(wifiAdapter);
        this.context.unregisterReceiver(groupManager);
    }

    private void setCurrentState(State currentState){
        this.previousState = this.currentState;
        this.currentState = currentState;
    }

    private void setPeerAsLeaf(){
        this.isParent = false;
        this.router = new LeafRouter(this);
        this.messenger = new MessageManager(this, this.router);

        WifiClientThread wifiClientThread = new WifiClientThread(this);

        wifiClientThread.start();

        this.startMulticastReceiverService();

    }

    private void setPeerAsParent(){
        this.isParent = true;
        this.router = new ParentRouter(this);
        this.messenger = new MessageManager(this, this.router);
        //TODO : ParentBroadcasting

        this.startMulticastReceiverService();

    }

    private void setPeerAsNull(){
        this.isParent = false;
        this.router = null;
        this.messenger = null;
    }

    public void sendMessage(String address, String message){
        if(this.messenger != null){
            this.messenger.sendMsg(message, address, Message.Algo.ROUTE);
        }
    }

    public void sendFLOOD(String message){
        if(this.messenger != null){
            this.messenger.sendMsg(message, Message.ALL, Message.Algo.FLOOD);
        }
    }

    public void sendCBS(String message){
        if(this.messenger!=null){
            this.messenger.sendMsg(message, Message.ALL, Message.Algo.CBS);
        }
    }

    ///============MULTICAST

    @Override
    public void onRawMulticastReceived(String multicastMessage) {
        this.handleRawJsonMessage(null, multicastMessage);
    }

    public void startMulticastReceiverService(){

        if(!MulticastReceiverService.isRunning){
            Intent multicastReceiverServiceIntent = new Intent(getActivity(), MulticastReceiverService.class);
            multicastReceiverServiceIntent.setAction(MulticastReceiverService.ACTION_LISTEN_FOR_MULTICAST);
            MulticastReceivedHandler multicastReceivedHandler = new MulticastReceivedHandler(this);
            multicastReceiverServiceIntent.putExtra(MulticastReceiverService.EXTRA_HANDLER_MESSENGER, new Messenger(multicastReceivedHandler));
            multicastReceiverServiceIntent.putExtra(MulticastReceiverService.IS_PARENT, this.isParent);

            getActivity().startService(multicastReceiverServiceIntent);
        }
    }

    //GETTERS

    public Context getContext(){
        return this.context;
    }

    //TODO : Check if needed
    public Activity getActivity() {
        return (Activity) this.padocInterface;
    }

    public String getBluetoothMac(){
        return this.localBluetoothMacAddress;
    }

    public UUID getPadocUUID(){
        return this.PADOC_UUID;
    }

    public Integer getRandomValue(){
        return this.randomValue;
    }

    public String getMeshUUID(){
        return this.meshUUID;
    }

    public String getSSID(){
        return  this.groupManager.getSSID();
    }

    public String getPass(){
        return this.groupManager.getPass();
    }

    public State getCurrentState(){
        return this.currentState;
    }

    public Set<Map.Entry<String, String>> getPeers(){

        return this.router != null ? this.router.getPeers() : null;
    }

    public int getHopsFor(String address){
        return this.router.getHopsFor(address);
    }

    //QUERIES

    public boolean isAcceptingBluetoothConnections(){
        return this.router.getNumberOfBluetoothConnections() < this.MAX_BLUETOOTH_CONNECTIONS;
    }

    public boolean isAcceptingWifiConnections(){
        return this.router.getNumberOfWifiConnections() < this.MAX_WIFI_CONNECTIONS;
    }

    //PadocListener

    public void print(Spanned spannedHtml, boolean time){
        this.padocInterface.print(spannedHtml, time);
    }

    ////

    public boolean isParent(){
        return this.isParent;
    }

}