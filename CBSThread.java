package com.react.gabriel.wbam.padoc;

/**
 * Created by gabriel on 24/05/16.
 */
public class CBSThread extends Thread {

    //In CBS, 0 < RAD < cbsMaxRAD
    //TODO : RAD should not be the same for every device, should it?
    //TODO : Sometimes when in triangle both RADs finish and both resend the info to each other, one of these will resend the CBS to the source
    //TODO : need to keep trace of the msg even after having sent it. In case we receive it again, like in the triangle configuration
    private static final int cbsMaxRAD = 2000;
    //TODO : Customize this value also
    private int nMax = 2;
    private int rad;

    private Messenger mMessenger;
    private final Message message;

    public CBSThread(Messenger mMessenger, Message message){

        this.mMessenger = mMessenger;
        this.message = message;
        this.rad = (int)(Math.random() * cbsMaxRAD);

    }

    public void run(){

        final String uuid = message.getUUID();

        System.out.println("RAD is : " + rad);

        try{
            Thread.sleep(rad);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        //How many duplicates?
        int n = mMessenger.getCBSCountForMsg(uuid);

        System.out.println("Count is n = " + n);

        if(n<nMax) {
            mMessenger.forwardBroadcast(message, mMessenger.getCBSBannedListForMsg(uuid));
        }

        mMessenger.clearCBSTrack(uuid);
    }

}
