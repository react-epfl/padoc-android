package com.react.gabriel.wbam.padoc.messaging;

import com.react.gabriel.wbam.padoc.Padoc;

/**
 * Created by gabriel on 24/05/16.
 */
public class CBSThread extends Thread {

    //In CBS, 0 < RAD < CBS_MAX_RAD
    //TODO : RAD should not be the same for every device, should it?
    //TODO : Sometimes when in triangle both RADs finish and both resend the info to each other, one of these will resend the CBS to the source
    //TODO : need to keep trace of the msg even after having sent it. In case we receive it again, like in the
    // triangle configuration
    private static final int CBS_MAX_RAD = 2000;
    //TODO : Customize this value also
    private int nMax = 2;
    private int rad;

    private Padoc padoc;
    private MessageManager mMessenger;
    private final Message message;

    public CBSThread(Padoc padoc, MessageManager messenger, Message message){

        this.padoc = padoc;
        this.mMessenger = messenger;
        this.message = message;
        this.rad = (int)(Math.random() * CBS_MAX_RAD);

    }

    public void run(){

        final String uuid = message.getUUID();

//        System.out.println("RAD is : " + rad);

        try{
            Thread.sleep(rad);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        if(this.padoc.isParent()){

            //How many bluetooth sources?
            int numberOfBluetoothSources = mMessenger.getCBSSourceCountFor(uuid);

//            this.padoc.print("Count of bluetoothSources is = " + numberOfBluetoothSources);

            if(numberOfBluetoothSources < nMax) {
//                this.padoc.print("forwarding to siblings");
                mMessenger.forwardCBSToSiblings(message, mMessenger.getCBSBannedListFor(uuid));
            }
        }

        int numberOfMulticasts = mMessenger.getCBSMulticastCountFor(uuid);

//        this.padoc.print("Count of multicasts is = " + numberOfMulticasts);

        if(numberOfMulticasts < nMax){
//            this.padoc.print("multicasting");

            //Multicast message
            this.mMessenger.forwardMulticast(message);
        }



        //TODO
//        try{
//            Thread.sleep(rad);
//        }catch (InterruptedException e){
//            e.printStackTrace();
//        }
//
//        mMessenger.clearCBSTrack(uuid);
    }

}
