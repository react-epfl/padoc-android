package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.PadocManager;
import com.react.gabriel.wbam.padoc.Router;

/**
 * Created by gabriel on 16/06/16.
 */
public class ConnectionsMonitor extends Thread {

    // triangle configuration
    private static final int TICK_TIME = 40000; //Must be > than TIMEOUT

    private static final int TIMEOUT = 10000;

    private PadocManager padocManager;
    private Router mRouter;

    public ConnectionsMonitor(PadocManager padocManager, Router router){

        this.padocManager = padocManager;
        this.mRouter = router;

    }

    public void run(){

        while (true){

            if(padocManager.isInMesh()){

                try {
                    Thread.sleep(TICK_TIME - TIMEOUT);

                }catch (InterruptedException e){
                    e.printStackTrace();
                }

                mRouter.resetACKLog();

                mRouter.requestACKs(padocManager.getLocalAddress());

                try {
                    Thread.sleep(TIMEOUT);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

                mRouter.cleanUpDirectConnections();

            }
        }

    }

}
