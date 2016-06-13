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
    private final int TICK_TIME = 500;
    private final int TOTAL_TIME = 60500;


    private MainActivity mActivity;

    public TestThread(MainActivity mActivity){

        this.mActivity = mActivity;

    }

    public void startTest(final Message.Algo algo){

        new CountDownTimer(TOTAL_TIME, TICK_TIME) {

            public void onTick(long millisUntilFinished) {

                switch (algo){
                    case FLOOD:

                        mActivity.sendFLOOD(null);

                        break;
                    case CBS:

                        mActivity.sendCBS(null);

                        break;
                    case ROUTE:

                        mActivity.msgBlack04(null);

                        break;
                }
            }

            public void onFinish() {
                mActivity.debugPrint("TEST DONE");
            }
        }.start();
    }

}
