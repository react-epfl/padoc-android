package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.PadocManager;
import com.react.gabriel.wbam.padoc.Router;

/**
 * Created by gabriel on 21/06/16.
 */
public class ACKMonitor extends Thread {

    // triangle configuration
    private static final int TICK_TIME = 20000;

    private PadocManager padocManager;
    private Router mRouter;

    public ACKMonitor(PadocManager padocManager, Router router){

        this.padocManager = padocManager;
        this.mRouter = router;

    }

    public void run(){

        while (true){

            if(padocManager.isInMesh()){

                try {
                    Thread.sleep(TICK_TIME);

                }catch (InterruptedException e){
                    e.printStackTrace();
                }

                mRouter.checkACKLog();

                mRouter.resetACKLog();

            }
        }

    }

}
