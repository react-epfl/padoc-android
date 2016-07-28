package com.react.gabriel.wbam.padoc.messaging.multicast;

import android.os.Handler;
import android.os.Message;

/**
 * Created by gabriel on 10/07/16.
 */
public class MulticastReceivedHandler extends Handler {

    public static final String RECEIVED_TEXT = "RECEIVED_TEXT";
    private MulticastReceivedListener multicastMessageReceivedListener;

    public MulticastReceivedHandler(MulticastReceivedListener multicastMessageReceivedListener) {
        this.multicastMessageReceivedListener = multicastMessageReceivedListener;
    }

    @Override
    public void handleMessage(Message messageFromMulticastMessageReceiverService) {

        String receivedText = getReceivedText(messageFromMulticastMessageReceiverService);

        multicastMessageReceivedListener.onRawMulticastReceived(receivedText);
    }

    private String getReceivedText(Message messageFromReceiverService) {
        return messageFromReceiverService.getData().getString(RECEIVED_TEXT);
    }
}
