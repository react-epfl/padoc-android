package com.react.gabriel.wbam.padoc;

import android.os.CountDownTimer;

import com.react.gabriel.wbam.MainActivity;

/**
 * Created by gabriel on 08/06/16.
 */
public class TestThread {

    //TWO SETTINGS
    //30 messages per minute for 10 minutes
//    private final int TICK_TIME = 2000;
//    private final int TOTAL_TIME = 602000;

    //120 messages per minute for 10 minutes
    private final int TICK_TIME = 2000;
    private final int TOTAL_TIME = 602000;


    private PadocManager padocManager;

    public TestThread(PadocManager padocManager){

        this.padocManager = padocManager;

    }

    public void startTest(final Message.Algo algo, Integer interval, final String destination){

        switch (algo){
            case FLOOD:

                new CountDownTimer(TOTAL_TIME, interval) {

                    public void onTick(long millisUntilFinished) {
                        padocManager.sendFLOOD();
                    }

                    public void onFinish() {
                        padocManager.debugPrint("FLOOD TEST DONE");
                    }
                }.start();

                break;
            case CBS:

                new CountDownTimer(TOTAL_TIME, interval) {

                    public void onTick(long millisUntilFinished) {
                        padocManager.sendCBS();
                    }

                    public void onFinish() {
                        padocManager.debugPrint("CBS TEST DONE");
                    }
                }.start();

                break;
            case ROUTE:

                new CountDownTimer(TOTAL_TIME, interval) {

                    public void onTick(long millisUntilFinished) {
                        padocManager.sendROUTE(destination);
                    }

                    public void onFinish() {
                        padocManager.debugPrint("ROUTE TEST DONE");
                    }
                }.start();

                break;
        }
    }

}
