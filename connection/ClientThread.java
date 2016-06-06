package com.react.gabriel.wbam.padoc.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;

/**
 * Created by gabriel on 18/05/16.
 */
public class ClientThread extends Thread {

    private final BluetoothManager btManager;
    private final BluetoothSocket mmSocket;
    private final String serverAddress;
    private final String serverName;

    public ClientThread(BluetoothManager btManager, BluetoothDevice device, String name) {

        this.btManager = btManager;
        this.serverAddress = device.getAddress();
        this.serverName = name;
        // Use a temporary object that is later assigned to mmSocket because mmSocket is final
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(btManager.getPadocUUID());
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    //Scanner handler
    private Handler connectionHandler = new Handler();
    //TODO: This number shouldn't be a constant.
    private final int TIMEOUT = 5000;

    //Scanner runnable
    private Runnable cancelConnection = new Runnable() {
        @Override
        public void run() {

            if(!mmSocket.isConnected()){
                try {
                    mmSocket.close();
                    btManager.connectionFailed(serverName, serverAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    };

    public void run() {
        // Cancel discovery because it will slow down the connection
//        if(btDiscovery.discoveryIsRunning) btDiscovery.stopDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            connectionHandler.postDelayed(cancelConnection, TIMEOUT);
            mmSocket.connect();

        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        if(mmSocket.isConnected()){
            btManager.manageConnectedSocket(serverName, mmSocket, serverAddress);
        }else {
            btManager.connectionFailed(serverName, serverAddress);
        }
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}