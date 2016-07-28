package com.react.gabriel.wbam.padoc.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.react.gabriel.wbam.padoc.Padoc;

import java.io.IOException;

/**
 * Created by gabriel on 18/05/16.
 */
public class BluetoothServerThread extends Thread {

    private final String PADOC_SERVICE = "padoc-service";

    private final Padoc padoc;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothServerSocket serverSocket;

    public BluetoothServerThread(Padoc padoc) {

        this.padoc = padoc;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothServerSocket tmp = null;

        while(tmp == null){
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = this.bluetoothAdapter.listenUsingRfcommWithServiceRecord(PADOC_SERVICE, padoc.getPadocUUID());
            } catch (IOException e) {
//                mActivity.debugPrint("Bluetooth Error! listenUsingRfcommWithServiceRecord failed. Reason:" + e);
            }
        }

        serverSocket = tmp;

    }

    public void run() {
//        padoc.print("Running Bluetooth server...");
        BluetoothSocket socket = null;
        int n = 0;
        // Keep listening until 4 connections are accepted
        while (true) {
//            mActivity.debugPrint("Waiting for client #" + n);
            try {
                if(serverSocket != null){
                    socket = serverSocket.accept();
                }else {
//                    padoc.print("ERROR : The server socket is null.");
                }
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Handle the connection in a separate thread
                padoc.handleBluetoothConnectedSocket(socket);
                n++;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException e) { }
    }
}