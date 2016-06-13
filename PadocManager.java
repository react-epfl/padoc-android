package com.react.gabriel.wbam.padoc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Pair;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.connection.BluetoothManager;
import com.react.gabriel.wbam.padoc.service.WifiDirectManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by gabriel on 25/05/16.
 */
public class PadocManager {

    private final UUID PADOC_UUID = UUID.fromString("aa40d6d0-16b0-11e6-bdf4-0800200c9a66");
    private final int MIN_RECOMMENDED_CONNECTIONS = 9;
    private final int MAX_RECOMMENDED_CONNECTIONS = 10;
    private String ALL = "ALL";

    public enum State{
        STATE_NULL,
        STATE_BLUETOOTH_RUNNING,
        STATE_WIFI_P2P_RUNNING,
        STATE_RUNNING,
        STATE_ATTEMPTING_CONNECTION,
        STATE_RECEIVING_CONNECTION;
    }

    private State state = State.STATE_NULL;

    private boolean DBG = true;
    private MainActivity mActivity;

    private String localName;

    private String localBluetoothAddress;
    private String localWDAddress;

    private Router mRouter;
    private Messenger mMessenger;

    //Bluetooth
    private BluetoothManager btManager;
    private IntentFilter btIntentFilter;

    //WifiDirect
    private WifiDirectManager wdManager;
    private IntentFilter wdIntentFilter;

    //Set containing addresses running PADOC
//    private Set<String> padocReadyDevices = new HashSet<String>();

    //Set containing peers addresses (first) and names (second) in the network
    private Set<Pair<String, String>> padocPeers = new HashSet<Pair<String, String>>();

    public PadocManager(MainActivity mActivity) {

        this.mActivity = mActivity;

        Intent intent = new Intent(this.mActivity.getBaseContext(), Monitor.class);

//        intent.putExtra("padocManager", (Parcelable) this);

        this.mActivity.startService(intent);

        //Bluetooth
        btManager = new BluetoothManager(mActivity, this);
        this.localBluetoothAddress = btManager.getLocalBluetoothAddress();
        this.localName = btManager.getLocalName();

        //Router
        this.mRouter = new Router();
        btManager.setRouter(mRouter);

        //Messenger
        this.mMessenger = new Messenger(mActivity, this, mRouter, localBluetoothAddress, localName);
        btManager.setMessenger(mMessenger);

        btIntentFilter = new IntentFilter();
        //Bluetooth state changes
        btIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //Bluetooth found a new device
        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //Bluetooth discovery finished
        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //Bluetooth detected new pairing state
        btIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //Bluetooth pairing request from another device
        btIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        //TODO : registerReceiver, here or in onResume()
        mActivity.registerReceiver(btManager, btIntentFilter);

        //WifiDirect
        wdManager = new WifiDirectManager(mActivity, this);

        wdIntentFilter = new IntentFilter();
        wdIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wdIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mActivity.registerReceiver(wdManager, wdIntentFilter);

//        initialize();
    }

    public void onDestroy(){
        mActivity.unregisterReceiver(btManager);
        mActivity.unregisterReceiver(wdManager);
    }

    //PADOC functions

    public void initialize(){

        if (btManager.getState().equals(BluetoothManager.State.STATE_NULL)
                && wdManager.getState().equals(WifiDirectManager.State.STATE_NULL)){

            this.state = State.STATE_NULL;
            btManager.initialize();

        }else if (btManager.getState().equals(BluetoothManager.State.STATE_RUNNING)
                && wdManager.getState().equals(WifiDirectManager.State.STATE_NULL)){

            this.state = State.STATE_BLUETOOTH_RUNNING;
            wdManager.initialize();

        }else if (btManager.getState().equals(BluetoothManager.State.STATE_NULL)
                && wdManager.getState().equals(WifiDirectManager.State.STATE_RUNNING)){

            //TODO : Complete
            this.state = State.STATE_WIFI_P2P_RUNNING;
        }else {

            //TODO
            this.state = State.STATE_RUNNING;
        }

    }

//    public boolean verifyPadocAddress(String address){
//        return padocReadyDevices.contains(address);
//    }

    //Bluetooth functions

//    public void startBluetoothDiscovery(){
//        btManager.startDiscovery();
//    }
//
//    public void stopBluetoothDiscovery(){
//        btManager.stopDiscovery();
//    }

    public void setBluetoothVisible(){
        btManager.setVisible();
    }

    public void setBluetoothInvisible(){
        btManager.setInvisible();
    }

    public void startBluetoothServer(){
        btManager.startServer();
    }

    public void stopBluetoothServer(){
        btManager.stopServer();
    }

    public void unpairBluetoothDevices(){
        btManager.unpairDevices();
    }

    public String getLocalBluetoothAddress(){
        return localBluetoothAddress;
    }

    public String getLocalName(){
        return this.localName;
    }

    //WifiDirect functions

    public void startWifiDirectService(){
        wdManager.startService(null);
    }

    public void forceStopWifiDirectService(){
        wdManager.forceStopService();
    }

    public void stopWifi(){
        wdManager.stopWifi();
    }

    public void startWifiDirectDiscovery(){
        wdManager.startDiscovery();
    }

    public void stopWifiDirectDiscovery(){
        wdManager.stopDiscovery();
    }

//    public void receivedNewConnection() {
//        //TODO
//        state = State.STATE_RECEIVING_CONNECTION;
//        wdManager.stopService();
//        wdManager.stopDiscovery();
//    }

    public void handleNewWifiDirectDiscovery(String name, String btAddress){
        //Called when a new Padoc device is discovered through Wifi-Direct

//        padocReadyDevices.add(btAddress);

        if(!mRouter.isConnectedTo(btAddress)
                && mRouter.numberOfActiveConnections() < MAX_RECOMMENDED_CONNECTIONS
                && !state.equals(State.STATE_ATTEMPTING_CONNECTION)
                && state.equals(State.STATE_RUNNING)){
            //We still don't have the minimum recommended number of connections.
            //We should attempt a connection to this device.

            state = State.STATE_ATTEMPTING_CONNECTION;
            wdManager.stopService();
            btManager.connectWith(name, btAddress);
        }else {
//            wdManager.stopDiscovery();
//            mActivity.debugPrint("Plop");
        }
    }

    public void connectionToServerSucceeded(String name, String macAddress){
        mActivity.debugPrint("Connection to " + name + " (" + macAddress + ") succeeded.");

        if(mRouter.numberOfActiveConnections() >= MIN_RECOMMENDED_CONNECTIONS) {
            //We are not looking to connect anymore, but we still accept connections

            wdManager.stopDiscovery();
        }

        wdManager.startService(null);
        state = State.STATE_RUNNING;
    }

    public void connectionFromClientSucceeded(){
        if(mRouter.numberOfActiveConnections() >= MIN_RECOMMENDED_CONNECTIONS){
            wdManager.stopDiscovery();
        }
    }

    public void connectionFailed(String name, String macAddress){
        //TODO : Need to add faulty server to list. Make 3 attempts max.
        mActivity.debugPrint("ERROR : Connection to " + name + " (" + macAddress + ") failed.");
        wdManager.startService(null);
        state = State.STATE_RUNNING;
    }

    public String[] getPeers(){

        Set<String> peers = mRouter.getPeers();

        String[] array = new String[peers.size()];

        return peers.toArray(array);
    }

    public Set<Map.Entry<String, String>> getPeerNames(){

        return mRouter.getPeerNamesAndHops();
    }

    public int getHopsFor(String address){
        return mRouter.getHopsFor(address);
    }

    public void sendMsg(String address){
        String msg = "Hello World";
        mMessenger.sendMsg(msg, address, Message.Algo.ROUTE);
    }

    //Debug functions
    public void debugPrint(String msg){
//        if (DBG) System.out.println(msg);
        mActivity.debugPrint(msg);
    }

    //Temporary functions

    public void sendCBS(){
        mMessenger.sendMsg("Hallo CBS from " + localName, ALL, Message.Algo.CBS);
    }

    public void sendFLOOD(){
        mMessenger.sendMsg("Test FLOOD from " + localName, ALL, Message.Algo.FLOOD);
    }

    public void sendMsgToBlack04(){
        String msg = "Hello World";
        mMessenger.sendMsg(msg, "B4:74:43:45:EB:78", Message.Algo.ROUTE);
    }

    //Getters

    public UUID getPadocUUID(){
        return this.PADOC_UUID;
    }

    public void showTestResults(){
        mMessenger.printMessageCount();
    }

    public void clearMessageCount(){
        mMessenger.clearMessageCount();
    }

}
