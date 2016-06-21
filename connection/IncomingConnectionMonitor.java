package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.PadocManager;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;

/**
 * Created by gabriel on 14/06/16.
 */
public class IncomingConnectionMonitor extends Thread {

    private static final int TIMEOUT = 12000;
    private final PadocManager padocManager;
    private final ConnectedThread connectedThread;

    public IncomingConnectionMonitor(PadocManager padocManager, ConnectedThread connectedThread){

        this.padocManager = padocManager;
        this.connectedThread = connectedThread;

    }

    public void run(){

        try{
            Thread.sleep(TIMEOUT);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        padocManager.identificationFromRemoteClientTimedOut(connectedThread);
    }

}
