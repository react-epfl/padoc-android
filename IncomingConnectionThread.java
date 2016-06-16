package com.react.gabriel.wbam.padoc;

import android.os.CountDownTimer;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;

/**
 * Created by gabriel on 14/06/16.
 */
public class IncomingConnectionThread extends Thread {

    private static final int TIMEOUT = 12000;
    private final PadocManager padocManager;
    private final ConnectedThread connectedThread;

    public IncomingConnectionThread(PadocManager padocManager, ConnectedThread connectedThread){

        this.padocManager = padocManager;
        this.connectedThread = connectedThread;

    }

    public void run(){

        try{
            Thread.sleep(TIMEOUT);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        padocManager.connectionFromRemoteClientTimedOut(connectedThread);
    }

}
