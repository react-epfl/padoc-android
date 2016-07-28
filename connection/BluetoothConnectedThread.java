package com.react.gabriel.wbam.padoc.connection;

import android.bluetooth.BluetoothSocket;
import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.messaging.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by gabriel on 18/05/16.
 */
public class BluetoothConnectedThread extends Thread implements ConnectedThread{

    private Padoc padoc;

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private String remoteAddress;
    private String remoteName;

    public BluetoothConnectedThread(Padoc padoc, BluetoothSocket socket) {

        this.padoc = padoc;
        this.mmSocket = socket;

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
        byte[] buffer = new byte[512];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream

                // Without Handler
                bytes = mmInStream.read(buffer);
                String jsonString = new String(buffer, 0, bytes);

                // TODO : what to deliver? Raw JSON string or Message?

                this.padoc.handleRawJsonMessage(this, jsonString);

//                Message message = new Message();
//                if(message.setMessage(jsonString))  this.padoc.deliverMsg(message, this);


            } catch (IOException e) {
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(Message message) {
        try {

            byte[] bytes = message.toString().getBytes();

            mmOutStream.write(bytes);

        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    @Override
    public void setRemoteAddress(String remoteAddress){
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void setRemoteName(String remoteName){
        this.remoteName = remoteName;
    }

    @Override
    public String getRemoteAddress(){
        return remoteAddress;
    }

    @Override
    public String getRemoteName(){
        return this.remoteName;
    }

    public boolean isOrphan(){
        return remoteAddress==null;
    }
}