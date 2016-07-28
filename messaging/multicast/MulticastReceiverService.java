package com.react.gabriel.wbam.padoc.messaging.multicast;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
public class MulticastReceiverService extends IntentService {

    public static final String TAG = MulticastReceiverService.class.getSimpleName();
    public static final String ACTION_LISTEN_FOR_MULTICAST = "ACTION_LISTEN_FOR_MULTICAST";
    public static final String EXTRA_HANDLER_MESSENGER = "EXTRA_HANDLER_MESSENGER";
    public static final String IS_PARENT = "IS_PARENT";
    public static boolean isRunning = false;
    public static boolean isParent;

//    public MulticastReceiverService(boolean isParent) {
//        super(MulticastReceiverService.class.getSimpleName());
//        this.isParent = isParent;
//    }

    public MulticastReceiverService() {
        super(MulticastReceiverService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        final String action = intent.getAction();
        if (action.equals(ACTION_LISTEN_FOR_MULTICAST)) {
            isParent = intent.getExtras().getBoolean(IS_PARENT);
            isRunning = true;
            try {
                MulticastSocket multicastSocket = createMulticastSocket();
                while (isRunning) {
                    DatagramPacket datagramPacket = createDatagramPacket();
                    multicastSocket.receive(datagramPacket);
                    sendReceivedDataToMulticastMessageReceivedHandler(getHandlerMessenger(intent), datagramPacket);
                }
            } catch (IOException | RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    private void sendReceivedDataToMulticastMessageReceivedHandler(Messenger handlerMessenger, DatagramPacket datagramPacket) throws RemoteException {
        Message handlerMessage = createHandlerMessage(getReceivedText(datagramPacket), getSenderIpAddress(datagramPacket));
        handlerMessenger.send(handlerMessage);
    }

    private Message createHandlerMessage(String receivedMessage, String senderIpAddress) {
        Bundle receivedData = new Bundle();
        receivedData.putString(MulticastReceivedHandler.RECEIVED_TEXT, receivedMessage);
//        receivedData.putString(MulticastReceivedHandler.SENDER_IP_ADDRESS, senderIpAddress);
        Message handlerMessage = new Message();
        handlerMessage.setData(receivedData);
        return handlerMessage;
    }

    private Messenger getHandlerMessenger(Intent intent) {
        return (Messenger) intent.getExtras().get(EXTRA_HANDLER_MESSENGER);
    }

    private String getSenderIpAddress(DatagramPacket datagramPacket) {
        return datagramPacket.getAddress().getHostAddress();
    }

    private String getReceivedText(DatagramPacket datagramPacket) {
        return new String(datagramPacket.getData(), 0, datagramPacket.getLength());
    }

    private MulticastSocket createMulticastSocket() throws IOException {
        MulticastSocket multicastSocket = new MulticastSocket(getPort());
        multicastSocket.setNetworkInterface(getNetworkInterface());
        multicastSocket.joinGroup(new InetSocketAddress(getMulticastGroupAddress(), getPort()), getNetworkInterface());
        return multicastSocket;
    }

    private NetworkInterface getNetworkInterface() throws SocketException {
        return NetworkUtil.getNetworkInterface(isParent);
    }

    private InetAddress getMulticastGroupAddress() throws UnknownHostException {
        return NetworkUtil.getMulticastGroupAddress();
    }

    private int getPort() {
        return NetworkUtil.getPort();
    }

    private DatagramPacket createDatagramPacket() {
        byte[] buffer = new byte[1024];
        return new DatagramPacket(buffer, buffer.length);
    }
}