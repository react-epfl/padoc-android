package com.react.gabriel.wbam.padoc.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.react.gabriel.wbam.padoc.Message;
import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.Messenger;
import com.react.gabriel.wbam.padoc.PadocManager;
import com.react.gabriel.wbam.padoc.Router;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * Created by gabriel on 03/05/16.
 */
public class BluetoothManager extends BroadcastReceiver{

    public enum State {
        STATE_NULL,
        STATE_ADAPTER_STARTING,
        STATE_ADAPTER_RUNNING,
        STATE_SERVER_STARTING,
        STATE_RUNNING;
    }

    private State state = State.STATE_NULL;

    private final MainActivity mActivity;
    private final PadocManager padocManager;
    private final BluetoothAdapter btAdapter;
    private final String localAddress;
    private Messenger mMessenger;
    private Router mRouter;

    //WBAM Devices that have already been paired and connected to should be kept paired, or not?
    private Set<BluetoothDevice> pairedDevices = null;

    //TODO : Should get visibility when starting, it may be already visible
    private boolean isVisible = false;

    //Server
    private ServerThread serverThread = null;

    private BluetoothDevice pairingDevicePriorToConnection = null;

    public BluetoothManager(MainActivity activity, PadocManager padocManager){

        this.mActivity = activity;
        this.padocManager = padocManager;

        this.btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null){
            //Device does not support Bluetooth

            mActivity.debugPrint("Device does not support Bluetooth.");
            this.localAddress = null;
//            this.btDiscovery = null;

        }else {
            //Device supports bluetooth

            //Set the local MAC Bluetooth address
            this.localAddress = btAdapter.getAddress();

            mActivity.debugPrint("This device is " + localAddress);

        }
    }

    public void initialize(){

        switch (state){
            case STATE_NULL :

                if(!btAdapter.isEnabled()){

                    if(btAdapter.enable()) this.state = State.STATE_ADAPTER_STARTING;

                }else {
                    //TODO : check if a reset is necessary

                    this.state = State.STATE_ADAPTER_RUNNING;
                    initialize();
                }

                break;
            case STATE_ADAPTER_STARTING :

                mActivity.debugPrint("Bluetooth adapter is starting...");

                break;
            case STATE_ADAPTER_RUNNING :

                state = State.STATE_SERVER_STARTING;


                this.pairedDevices = btAdapter.getBondedDevices();
                if(pairedDevices.size() > 0){
                    mActivity.debugPrint("Found " + pairedDevices.size() + " paired BT devices");
                }

                serverThread = new ServerThread(mActivity, this, btAdapter);

                initialize();

                break;
            case STATE_SERVER_STARTING :

                serverThread.start();

                state = State.STATE_RUNNING;

                padocManager.initialize();

                break;
            case STATE_RUNNING:

                mActivity.debugPrint("Bluetooth server is running.");

                padocManager.initialize();

                break;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();


        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch(state) {
                case BluetoothAdapter.STATE_OFF:
                    mActivity.debugPrint("Bluetooth is OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    mActivity.debugPrint("Bluetooth is turning OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    mActivity.debugPrint("Bluetooth is ON");

                    if(this.state.equals(State.STATE_ADAPTER_STARTING)){
                        this.state = State.STATE_ADAPTER_RUNNING;
                        initialize();
                    }

                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    mActivity.debugPrint("Bluetooth is turning ON");
                    break;
            }

        }else if(action.equals(BluetoothDevice.ACTION_FOUND)){
            //When discovery finds a device
/*
            //Get the Bluetooth device
            BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            //The only thing needed to connect to a device is the MAC address
            if(btDevice != null) handleNewBTDiscovery(btDevice);
*/
        }else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
            //Bluetooth discovery finished, relaunch?
/*
            if(btDiscovery.discoveryIsRunning) btDiscovery.runDiscoveryOnce();
*/
        }else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

            //Bluetooth pairing state changed.
            BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

            if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                mActivity.debugPrint("Paired " + btDevice.getName());
                if(pairingDevicePriorToConnection != null && pairingDevicePriorToConnection.equals(btDevice)){
                    connectTo(btDevice.getName(), btDevice);
                }

            } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                mActivity.debugPrint("Unpaired " + btDevice.getName());
            }

        }else if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){

            //Received a pairing request
            //TODO : sometimes, the pairing dialog is not dismissed
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int pin = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 0);
            //the pin in case you need to accept for an specific pin
            mActivity.debugPrint(" " + pin);
            //maybe you look for a name or address
            mActivity.debugPrint(remoteDevice.getName());
            try {
                byte[] pinBytes = Integer.toString(pin).getBytes("UTF-8");
                //From API 19 we don't need reflection anymore
                remoteDevice.setPin(pinBytes);
                //TODO dialog stays up sometimes.
                remoteDevice.setPairingConfirmation(true);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //setPairing confirmation if needed
            remoteDevice.setPairingConfirmation(true);
        }
    }

    //TODO : unnecessary
//    public void startDiscovery(){
//        btDiscovery.startDiscovery();
//    }
//
//    //TODO : unnecessary
//    public void stopDiscovery(){
//        btDiscovery.stopDiscovery();
//    }

    public void startServer(){
        //Do not use run(), it just runs the code, it does not create a new Thread
        if(serverThread != null && !serverThread.isAlive()){
            serverThread.start();
        }
    }

    public void stopServer(){
        serverThread.cancel();
    }

    /**
     * You should always ensure that the device is not performing device discovery when you call connect().
     * If discovery is in progress, then the connection attempt will be significantly slowed and is more likely to fail.
     * @param btDevice
     */
    public void connectTo(String name, BluetoothDevice btDevice){
        mActivity.debugPrint("Connecting to " + btDevice.getName());

        ClientThread clientThread = new ClientThread(this, btDevice, name);
        clientThread.start();
    }

    //TODO : unnecessary
    /**
     * By default, the device will become discoverable for 120 seconds.
     * You can define a different duration by adding the EXTRA_DISCOVERABLE_DURATION Intent extra.
     * The maximum duration an app can set is 3600 seconds, and a value of 0 means the device is
     * always discoverable.
     */
    public void setVisible(){
        if(!isVisible){
            Method method;
            try {
                method = btAdapter.getClass().getMethod("setScanMode", int.class, int.class);
                method.invoke(btAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
                isVisible = true;
                mActivity.debugPrint("BT is now visible");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }else {
            mActivity.debugPrint("Error : BT is already visible");
        }
    }

    //TODO : unnecessary
    //Done using reflection
    //http://grepcode.com/file/repo1.maven.org/maven2/org.robolectric/android-all/5.0.0_r2-robolectric-0/android/bluetooth/BluetoothAdapter.java#BluetoothAdapter.setScanMode%28int%2Cint%29
    public void setInvisible(){
        if(isVisible){
            Method method;
            try {
                method = btAdapter.getClass().getMethod("setScanMode", int.class);
                method.invoke(btAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE);
                isVisible = false;
                mActivity.debugPrint("BT is now hidden");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        } else {
            mActivity.debugPrint("Error : BT is already hidden");
        }
    }

    private boolean pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
            return false;
        }
    }

    //TODO : unnecessary
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO : unnecessary
    public void unpairDevices(){
        for(BluetoothDevice btDevice : btAdapter.getBondedDevices()){
            //For now... unpair all devices
            unpairDevice(btDevice);
        }
    }

    //TODO : unnecessary
//    private void handleNewBTDiscovery(BluetoothDevice btDevice){
//
//        mActivity.debugPrint("Discovered : " + btDevice.getName());
//
//        String btAddress = btDevice.getAddress();
//        if(padocManager.verifyPadocAddress(btAddress)){
//
//            //Attempting connection to WHITE
//            pairingDevicePriorToConnection = btDevice;
//            //Stop discovery
//            stopDiscovery();
//            //Pair with WHITE
//            pairDevice(btDevice);
//        }
//
//    }

    /**
     * Creates either an orphanThread if the connection comes from the serverThread
     * or a connectedThread if the connection comes from a clientThread
     * @param btSocket
     * @param remoteAddress
     */
    public void manageConnectedSocket(String name, BluetoothSocket btSocket, String remoteAddress){

        if(remoteAddress!=null){
            //From clientThread

            ConnectedThread connectedThread = new ConnectedThread(this, btSocket, remoteAddress);
            connectedThread.start();

            mRouter.setConnectedDevice(name, remoteAddress, connectedThread);

            padocManager.connectionSucceeded(name, remoteAddress);

            mMessenger.introduceMyselfToThread(connectedThread);

        }else{
            //From serverThread

            ConnectedThread connectedThread = new ConnectedThread(this, btSocket, null);
            connectedThread.start();

            mRouter.setOrphanThread(connectedThread);
            mActivity.debugPrint("Unidentified peer just connected.");
        }
    }

    public void connectionFailed(String name, String macAddress){
        padocManager.connectionFailed(name, macAddress);
    }

    public void connectWith(String name, String btMacAddress){

        Gson gson = new Gson();

        String jsonString = "{\"mAddress\":\"" + btMacAddress + "\"}";
        BluetoothDevice btDevice = gson.fromJson(jsonString, BluetoothDevice.class);

        if(pairedDevices.contains(btDevice)) {
            connectTo(name, btDevice);
        }else {
            pairingDevicePriorToConnection = btDevice;
            pairDevice(btDevice);
        }

    }

    public String getLocalAddress(){
        return this.localAddress;
    }

//    public boolean discoveryIsRunning(){
//        return btDiscovery.discoveryIsRunning;
//    }


    public void deliverMsg(Message message, ConnectedThread connectedThread){
        mMessenger.deliverMsg(message, connectedThread);
    }

    //ROUTER
    public void setRouter(Router router){
        this.mRouter = router;
    }

    public boolean knows(String address){
        return mRouter.knows(address);
    }

    //MESSENGER
    public void setMessenger(Messenger messenger){
        this.mMessenger = messenger;
    }

    public void introduceMeshTo(String address){
        mMessenger.sendMeshInfoTo(address);
    }

    //Getters
    public String getLocalBluetoothAddress(){
        return this.localAddress;
    }

    public String getLocalName(){
        return btAdapter.getName();
    }

    public State getState(){
        return this.state;
    }

    public void setState(State state){

        this.state = state;

    }

    public boolean isRunning(){
        return this.state.equals(State.STATE_RUNNING);
    }

    public UUID getPadocUUID(){
        return padocManager.getPadocUUID();
    }
}