package com.react.gabriel.wbam.padoc.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gabriel on 18/05/16.
 */
public class ClientThread extends Thread {

    private final BluetoothManager btManager;
    private final BluetoothSocket mmSocket;
    private final BluetoothDiscovery btDiscovery;
    private final String serverAddress;

    public ClientThread(BluetoothManager btManager, BluetoothDevice device, BluetoothDiscovery btDiscovery) {

        this.btManager = btManager;
        this.btDiscovery = btDiscovery;
        this.serverAddress = device.getAddress();
        // Use a temporary object that is later assigned to mmSocket because mmSocket is final
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(btManager.getPadocUUID());
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        if(btDiscovery.discoveryIsRunning) btDiscovery.stopDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        btManager.manageConnectedSocket(mmSocket, serverAddress);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
