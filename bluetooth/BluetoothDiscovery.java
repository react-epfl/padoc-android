package com.react.gabriel.wbam.padoc.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.react.gabriel.wbam.MainActivity;

/**
 * Created by gabriel on 13/05/16.
 */
public class BluetoothDiscovery {

    private MainActivity mActivity = null;
    private BluetoothAdapter btAdapter = null;
    public boolean discoveryIsRunning = false;


    public BluetoothDiscovery(MainActivity activity, BluetoothAdapter adapter){
        this.mActivity = activity;
        this.btAdapter = adapter;
    }

    /**
     * Caution: Performing device discovery is a heavy procedure for the Bluetooth adapter and will
     * consume a lot of its resources. Once you have found a device to connect, be certain that you
     * always stop discovery with cancelDiscovery() before attempting a connection.
     * Also, if you already hold a connection with a device, then performing discovery can
     * significantly reduce the bandwidth available for the connection, so you should not
     * perform discovery while connected.
     * @return
     */
    public void runDiscoveryOnce(){
        if(btAdapter.startDiscovery()){
            mActivity.debugPrint("BT scan...");
        }else {
            mActivity.debugPrint("BT scan failed.");
            discoveryIsRunning = false;
        }
    }

    public void startDiscovery(){
        if(!discoveryIsRunning){
            discoveryIsRunning = true;
            mActivity.debugPrint("Starting BT discovery");
            runDiscoveryOnce();
        }else{
            mActivity.debugPrint("Error : BT discovery is already running");
        }
    }

    public void stopDiscovery(){
        if(discoveryIsRunning){
            discoveryIsRunning = false;
            mActivity.debugPrint("BT discovery stopped");
            btAdapter.cancelDiscovery();
        }else {
            mActivity.debugPrint("Error : BT discovery is not running");
        }
    }
}
