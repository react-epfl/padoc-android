package com.react.gabriel.wbam.padoc.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.react.gabriel.wbam.MainActivity;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gabriel on 18/05/16.
 */
public class ServerThread extends Thread {

    private final UUID WBAM_BT_UUID = UUID.fromString("aa40d6d0-16b0-11e6-bdf4-0800200c9a66");
    private final String WBAM_BT_SERVICE = "WBAM_BT";

    private final MainActivity mActivity;
    private final BluetoothManager btManager;
    private final BluetoothAdapter btAdapter;
    private final BluetoothServerSocket mmServerSocket;

    public ServerThread(MainActivity mActivity, BluetoothManager btManager, BluetoothAdapter btAdapter) {

        this.mActivity = mActivity;
        this.btManager = btManager;
        this.btAdapter = btAdapter;

        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = btAdapter.listenUsingRfcommWithServiceRecord(WBAM_BT_SERVICE, WBAM_BT_UUID);
        } catch (IOException e) { }
        mmServerSocket = tmp;
    }

    public void run() {
        mActivity.debugPrint("Running server...");
        BluetoothSocket socket = null;
        int n = 0;
        // Keep listening until 4 connections are accepted
        while (n<4) {
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
                btManager.manageConnectedSocket(socket, null);
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