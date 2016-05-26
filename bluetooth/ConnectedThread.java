package com.react.gabriel.wbam.padoc.bluetooth;

import android.bluetooth.BluetoothSocket;

import com.react.gabriel.wbam.padoc.JsonMsg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by gabriel on 18/05/16.
 */
public class ConnectedThread extends Thread {

    private BluetoothManager btManager = null;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private String remoteAddress;

    public ConnectedThread(BluetoothManager btManager, BluetoothSocket socket, String remoteAddress) {

        this.btManager = btManager;
        this.mmSocket = socket;
        this.remoteAddress = remoteAddress;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mmSocket.getInputStream();
            tmpOut = mmSocket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream

                //Without Handler
                bytes = mmInStream.read(buffer);
                String jsonString = new String(buffer, 0, bytes);
                JsonMsg jsonMsg = new JsonMsg(jsonString);
                btManager.deliverMsg(jsonMsg, this);

            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(JsonMsg jsonMsg) {
        try {
            System.out.println("Sending " + jsonMsg.toString());
            byte[] bytes = jsonMsg.toString().getBytes();
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    public String getRemoteAddress(){
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress){
        this.remoteAddress = remoteAddress;
    }

    public boolean isOrphan(){
        return remoteAddress==null;
    }
}