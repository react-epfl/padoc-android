package com.react.gabriel.wbam.padoc.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.react.gabriel.wbam.padoc.JsonMsg;
import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.Messenger;
import com.react.gabriel.wbam.padoc.PadocManager;
import com.react.gabriel.wbam.padoc.Router;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by gabriel on 03/05/16.
 */
public class BluetoothManager extends BroadcastReceiver{

    private final MainActivity mActivity;
    private final PadocManager padocManager;
    private final BluetoothAdapter btAdapter;
    private final String localAddress;
    private final BluetoothDiscovery btDiscovery;
    private Messenger mMessenger;
    private Router mRouter;

    //WBAM Devices that have already been paired and connected to should be kept paired, or not?
    private Set<BluetoothDevice> pairedDevices = null;

    //TODO : Should get visibility when starting, it may be already visible
    private boolean isVisible = false;

    //Server
    private ServerThread serverThread = null;

    private BluetoothDevice connectingToDevice = null;

    public BluetoothManager(MainActivity activity, PadocManager padocManager){

        this.mActivity = activity;
        this.padocManager = padocManager;

        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null){
            //Device does not support Bluetooth
            mActivity.debugPrint("Device does not support Bluetooth.");
            this.localAddress = null;
            this.btDiscovery = null;

        }else {
            btAdapter.enable();
            this.localAddress = btAdapter.getAddress();

            mActivity.debugPrint("This device is " + localAddress);

            this.btDiscovery = new BluetoothDiscovery(mActivity, btAdapter);
            this.serverThread = new ServerThread(mActivity, this, btAdapter);
            //TODO I should use these sometime and filter only WBAM paired devices
            this.pairedDevices = btAdapter.getBondedDevices();
            if(pairedDevices.size() > 0){
                mActivity.debugPrint("Found " + pairedDevices.size() + " paired BT devices");
            }
        }
    }

    public void startDiscovery(){
        btDiscovery.startDiscovery();
    }

    public void stopDiscovery(){
        btDiscovery.stopDiscovery();
    }

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
    public void connectTo(BluetoothDevice btDevice){
        mActivity.debugPrint("Connecting to " + btDevice.getName());
        (new ClientThread(this, btDevice, btDiscovery)).start();
    }

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
            return false;
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unpairDevices(){
        for(BluetoothDevice btDevice : btAdapter.getBondedDevices()){
            if(btDevice.getName().contains("WHITE") || btDevice.getName().contains("BLUE") || btDevice.getName().contains("Galaxy")){
                unpairDevice(btDevice);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        //When discovery finds a device
        if(action.equals(BluetoothDevice.ACTION_FOUND)){
            //Get the Bluetooth device
            BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //The only thing needed to connect to a device is the MAC address
            if(btDevice != null) handleNewBTDiscovery(btDevice);

        }else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        {
            //Bluetooth discovery finished, relaunch?
            if(btDiscovery.discoveryIsRunning) btDiscovery.runDiscoveryOnce();

        }else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

            //Bluetooth pairing state changed.
            BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

            if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                mActivity.debugPrint("Paired " + btDevice.getName());
                if(connectingToDevice != null && connectingToDevice.equals(btDevice)){
                    connectTo(btDevice);
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

    /**
     * Creates either an orphanThread if the connection comes from the serverThread
     * or a connectedThread if the connection comes from a clientThread
     * @param btSocket
     * @param remoteAddress
     */
    public void manageConnectedSocket(BluetoothSocket btSocket, String remoteAddress){

        if(remoteAddress!=null){
            //From clientThread

            ConnectedThread connectedThread = new ConnectedThread(this, btSocket, remoteAddress);
            connectedThread.start();

            mRouter.setConnectedDevice(remoteAddress, connectedThread);

            mActivity.debugPrint("Connected to " + remoteAddress);

            mMessenger.introduceMyselfToThread(connectedThread);

        }else{
            //From serverThread

            ConnectedThread connectedThread = new ConnectedThread(this, btSocket, null);
            connectedThread.start();

            mRouter.setOrphanThread(connectedThread);
            mActivity.debugPrint("Connected to unidentified peer");
        }
    }

    private void handleNewBTDiscovery(BluetoothDevice btDevice){

        mActivity.debugPrint("Discovered : " + btDevice.getName());

//        Gson gson = new Gson();
//        String btDeviceString = gson.toJson(btDevice);
//        mActivity.debugPrint("gson : " + btDeviceString);
//
//        BluetoothDevice btD = gson.fromJson(btDeviceString, BluetoothDevice.class);
//        mActivity.debugPrint(btD.toString());
//
//        String fakeS = "{\"mAddress\":\"11:22:33:44:55:66\"}";
//        BluetoothDevice fakeD = gson.fromJson(fakeS, BluetoothDevice.class);
//        mActivity.debugPrint(fakeD.toString());
//        String fakeS2 = gson.toJson(fakeD);
//        mActivity.debugPrint("rebuild:"+fakeS2);

        //TODO recognize WBAM devices

        String btAddress = btDevice.getAddress();
        if(padocManager.verifyPadocAddress(btAddress)){

            //Attempting connection to WHITE
            connectingToDevice = btDevice;
            //Stop discovery
            stopDiscovery();
            //Pair with WHITE
            pairDevice(btDevice);
        }

//        if(btDevice.getName() != null && (btDevice.getName().contains("WHITE") || btDevice.getName().contains("BLUE") || btDevice.getName().contains("Galaxy"))){
//
//            //Attempting connection to WHITE
//            connectingToDevice = btDevice;
//            //Stop discovery
//            stopDiscovery();
//            //Pair with WHITE
//            pairDevice(btDevice);
//        }
    }

    public String getLocalAddress(){
        return this.localAddress;
    }

    public boolean discoveryIsRunning(){
        return btDiscovery.discoveryIsRunning;
    }


    public void deliverMsg(JsonMsg jsonMsg, ConnectedThread connectedThread){
        mMessenger.deliverMsg(jsonMsg, connectedThread);
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
}