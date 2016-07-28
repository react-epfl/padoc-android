package com.react.gabriel.wbam.padoc.messaging;

import android.os.CountDownTimer;

import com.react.gabriel.wbam.padoc.Padoc;

/**
 * Created by gabriel on 08/06/16.
 */
public class TestThread {

    //TWO SETTINGS
    //30 messages per minute for 10 minutes
//    private final int TICK_TIME = 2000;
//    private final int TOTAL_TIME = 602000;

    //120 messages per minute for 1 minutes
//    private final int TICK_TIME = 2000;
    private final int TOTAL_TIME = 600000;

    private Padoc padoc;

    public TestThread(Padoc padoc){

        this.padoc = padoc;

    }

    public void startTest(final Message.Algo algo, Integer interval, final String destination){

        switch (algo){
            case FLOOD:

                new CountDownTimer(TOTAL_TIME + interval, interval) {

                    public void onTick(long millisUntilFinished) {
                        padoc.sendFLOOD("This is a flood");
                    }

                    public void onFinish() {
//                        padoc.print("FLOOD TEST DONE");
                    }
                }.start();

                break;
            case CBS:

                new CountDownTimer(TOTAL_TIME + interval, interval) {

                    public void onTick(long millisUntilFinished) {
                        padoc.sendCBS("This is a CBS");
                    }

                    public void onFinish() {
//                        padoc.print("CBS TEST DONE");
                    }
                }.start();

                break;
            case ROUTE:

                new CountDownTimer(TOTAL_TIME + interval, interval) {

                    public void onTick(long millisUntilFinished) {
                        padoc.sendMessage(destination, "This is a route");
                    }

                    public void onFinish() {
//                        padoc.print("ROUTE TEST DONE");
                    }
                }.start();

                break;
        }
    }

}
