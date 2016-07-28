package com.react.gabriel.wbam.padoc.broadcastreceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.connection.BluetoothClientThread;
import com.react.gabriel.wbam.padoc.connection.BluetoothServerThread;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

/**
 * Created by gabriel on 03/05/16.
 */
public class BluetoothManager extends BroadcastReceiver {

    public enum State {
        STATE_NULL,
        STATE_ADAPTER_STARTING,
        STATE_ADAPTER_RUNNING,
        STATE_SERVER_STARTING,
        STATE_RUNNING;
    }

    private State state = State.STATE_NULL;

    private final Padoc padoc;

    private final BluetoothAdapter btAdapter;
    private final String localName;
    private final String localAddress;

    //WBAM Devices that have already been paired and connected to should be kept paired, or not?
    private Set<BluetoothDevice> pairedDevices = null;

    //TODO : Should get visibility when starting, it may be already visible
    private boolean isVisible = false;

    //Server
    private BluetoothServerThread bluetoothServerThread = null;

    private BluetoothDevice pairingDevicePriorToConnection = null;

    public BluetoothManager(Padoc padoc){

        this.padoc = padoc;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null){
            //Device does not support Bluetooth

//            padoc.print("Device does not support Bluetooth.");
            this.localName = null;
            this.localAddress = null;

        }else {
            //Device supports bluetooth
            //Set the local MAC Bluetooth address
            this.localName = btAdapter.getName();
            this.localAddress = btAdapter.getAddress();

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

//                this.padoc.print("Bluetooth adapter is starting...");

                break;
            case STATE_ADAPTER_RUNNING :

                state = State.STATE_SERVER_STARTING;

                this.pairedDevices = btAdapter.getBondedDevices();

//                if(pairedDevices.size() > 0){
//                    this.padoc.print("Found " + pairedDevices.size() + " paired BT devices");
//                }

                this.bluetoothServerThread = new BluetoothServerThread(this.padoc);

                initialize();

                break;
            case STATE_SERVER_STARTING :

                this.bluetoothServerThread.start();

                state = State.STATE_RUNNING;

                this.padoc.onBluetoothServerRunning();

                break;
            case STATE_RUNNING:

//                mActivity.debugPrint("Bluetooth server is running.");
//                padocManager.initialize();
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
//                    this.padoc.print("Bluetooth is OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
//                    this.padoc.print("Bluetooth is turning OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
//                    this.padoc.print("Bluetooth is ON");

                    if(this.state.equals(State.STATE_ADAPTER_STARTING)){
                        this.state = State.STATE_ADAPTER_RUNNING;
                        initialize();
                    }

                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
//                    this.padoc.print("Bluetooth is turning ON");
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
//                this.padoc.print("Paired " + btDevice.getName());
                if(pairingDevicePriorToConnection != null && pairingDevicePriorToConnection.equals(btDevice)){
                    connectTo(btDevice);
                }

            } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
//                this.padoc.print("Unpaired " + btDevice.getName());
            }

        }else if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){

            //Received a pairing request
            //TODO : sometimes, the pairing dialog is not dismissed
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int pin = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, 0);
            //the pin in case you need to accept for an specific pin
//            this.padoc.print(" " + pin);
            //maybe you look for a name or address
//            this.padoc.print(remoteDevice.getName());
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

    public String getLocalName(){
        return this.localName;
    }

    public String getLocalAddress(){
        return this.localAddress;
    }

    public void startBluetoothServer(){
//        bluetoothServerThread = new BluetoothServerThread(padoc, this, btAdapter);
    }

    public void connectionToRemoteServerFailed(String mesh, String name, String macAddress){
        //TODO : Need to add faulty server to list. Make 3 attempts max.
//        padoc.print("ERROR : Connection to " + name + " failed.");

//        this.setState(State.STATE_RUNNING);
    }

    public void attemptConnectionWith(String btMacAddress){

        Gson gson = new Gson();

        String jsonString = "{\"mAddress\":\"" + btMacAddress + "\"}";
        BluetoothDevice btDevice = gson.fromJson(jsonString, BluetoothDevice.class);

        if(pairedDevices.contains(btDevice)) {
//            padoc.print("Already paired");
            connectTo(btDevice);
        }else {
            pairDevice(btDevice);
        }
    }

    private boolean pairDevice(BluetoothDevice device) {
        try {
            this.pairingDevicePriorToConnection = device;
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
            return false;
        }
    }

    public void connectTo(BluetoothDevice btDevice){
//        padoc.print("Connecting to " + btDevice.getName());

        BluetoothClientThread bluetoothClientThread = new BluetoothClientThread(this.padoc, btDevice);
        bluetoothClientThread.start();
    }

    public UUID getPadocUUID(){
        return this.padoc.getPadocUUID();
    }
}