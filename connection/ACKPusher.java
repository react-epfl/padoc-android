package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.Messenger;
import com.react.gabriel.wbam.padoc.PadocManager;
import com.react.gabriel.wbam.padoc.Router;

/**
 * Created by gabriel on 21/06/16.
 */
public class ACKPusher extends Thread {

    private static final int TICK_TIME = 20000;

    private PadocManager padocManager;
    private Messenger mMessenger;

    public ACKPusher(PadocManager padocManager, Messenger mMessenger){

        this.padocManager = padocManager;
        this.mMessenger = mMessenger;

    }

    public void run(){

        while (true){

            if(padocManager.isInMesh()){

                try {
                    Thread.sleep(TICK_TIME);

                }catch (InterruptedException e){
                    e.printStackTrace();
                }

                mMessenger.pushACKs();

            }
        }

    }

}
