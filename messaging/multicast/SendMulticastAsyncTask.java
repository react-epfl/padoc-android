package com.react.gabriel.wbam.padoc.messaging.multicast;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by gabriel on 10/07/16.
 */
public class SendMulticastAsyncTask extends AsyncTask<Void, String, Boolean> {

    public static final String TAG = SendMulticastAsyncTask.class.getSimpleName();
    private MulticastSentListener multicastSentListener;
//    private UserInputHandler userInputHandler;
    private String messageToBeSent;
    private final boolean isParent;

    public SendMulticastAsyncTask(MulticastSentListener multicastSentListener, String msg, boolean isParent) {
        this.multicastSentListener = multicastSentListener;
//        this.userInputHandler = userInputHandler;
        this.messageToBeSent = msg;
        this.isParent = isParent;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        boolean success = false;
        try {
            MulticastSocket multicastSocket = createMulticastSocket();

//            String messageToBeSent = userInputHandler.getMessageToBeSentFromUserInput();

            DatagramPacket datagramPacket = new DatagramPacket(messageToBeSent.getBytes(), messageToBeSent.length(), getMulticastGroupAddress(), getPort());
            multicastSocket.send(datagramPacket);
            success = true;
        } catch (IOException ioException) {
            Log.e(TAG, ioException.toString());
        }
        return success;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success) {
            multicastSentListener.onCouldNotSendMulticast();
        }
//        userInputHandler.clearUserInput();
    }

    private MulticastSocket createMulticastSocket() throws IOException {
        MulticastSocket multicastSocket = new MulticastSocket(getPort());
        multicastSocket.setNetworkInterface(getNetworkInterface());
        multicastSocket.joinGroup(new InetSocketAddress(getMulticastGroupAddress(), getPort()), getNetworkInterface());
        return multicastSocket;
    }

    private NetworkInterface getNetworkInterface() throws SocketException {
        return NetworkUtil.getNetworkInterface(this.isParent);
    }

    private InetAddress getMulticastGroupAddress() throws UnknownHostException {
        return NetworkUtil.getMulticastGroupAddress();
    }

    private int getPort() {
        return NetworkUtil.getPort();
    }


}