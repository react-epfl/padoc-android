package com.react.gabriel.wbam.padoc.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.react.gabriel.wbam.MainActivity;

import java.io.IOException;

/**
 * Created by gabriel on 18/05/16.
 */
public class ServerThread extends Thread {

    private final String PADOC_SERVICE = "padoc-service";
    private final int maxClientsConnections = 7;

    private final MainActivity mActivity;
    private final BluetoothManager btManager;
    private final BluetoothServerSocket mmServerSocket;

    public ServerThread(MainActivity mActivity, BluetoothManager btManager, BluetoothAdapter btAdapter) {

        this.mActivity = mActivity;
        this.btManager = btManager;

        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;

        while(tmp == null){
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(PADOC_SERVICE, btManager.getPadocUUID());
            } catch (IOException e) {
//                mActivity.debugPrint("Bluetooth Error! listenUsingRfcommWithServiceRecord failed. Reason:" + e);
            }
        }

        mmServerSocket = tmp;

    }

    public void run() {
        mActivity.debugPrint("Running server...");
        BluetoothSocket socket = null;
        int n = 0;
        // Keep listening until 4 connections are accepted
        while (n < maxClientsConnections) {
            mActivity.debugPrint("Waiting for client #" + n);
            try {
                if(mmServerSocket != null){
                    socket = mmServerSocket.accept();
                }else {
                    mActivity.debugPrint("ERROR : The server socket is null.");
                }
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                btManager.manageConnectedSocket(null, socket, null);
                n++;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }
}