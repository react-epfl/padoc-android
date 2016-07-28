package com.react.gabriel.wbam.padoc.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;

import com.react.gabriel.wbam.padoc.Padoc;

import java.util.List;
import java.util.Random;

/**
 * Created by gabriel on 28/06/16.
 */
public class WifiAdapter extends BroadcastReceiver {

    private final Integer WIFI_SETTINGS_REQUEST_CODE = (new Random()).nextInt(255);

    public enum State {
        STATE_NULL,
        STATE_WIFI_ADAPTER_STARTING,
        STATE_WIFI_ADAPTER_RESETTING,
        STATE_WIFI_ADAPTER_STOPPED,
        STATE_WIFI_ADAPTER_ENABLED;
    }

    private State currentState;
    private State previousState;

    private final Padoc padoc;
    private final Context context;

    private final WifiManager wifiManager;

    public WifiAdapter(Padoc padoc){

        this.padoc = padoc;
        this.context = padoc.getContext();
        this.currentState = State.STATE_NULL;
        this.previousState = State.STATE_NULL;
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    public void onReceive(Context context, Intent intent){

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Have access to: EXTRA_WIFI_STATE
            // Constant Value: "android.net.wifi.p2p.STATE_CHANGED"

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                if(this.currentState.equals(State.STATE_WIFI_ADAPTER_STARTING)){
                    this.setCurrentState(State.STATE_WIFI_ADAPTER_ENABLED);
                    this.iterate();
                }

            } else {
                // Wi-Fi P2P is not enabled
                if(this.currentState.equals(State.STATE_WIFI_ADAPTER_RESETTING)){
                    this.setCurrentState(State.STATE_WIFI_ADAPTER_STOPPED);
                    this.iterate();
                }

            }
        }
    }

    //LIFECYCLE

    private void iterate(){
        switch (this.currentState){
            case STATE_NULL:

                if(!wifiManager.isWifiEnabled()){
                    //Wifi is not enabled
                    setCurrentState(State.STATE_WIFI_ADAPTER_STARTING);
                    enableWifiAdapter();

                }else {
                    //Wifi is already enabled, reset it.
                    setCurrentState(State.STATE_WIFI_ADAPTER_RESETTING);
                    stopWifiAdapter();
                }

                break;
            case STATE_WIFI_ADAPTER_STOPPED:

                if (this.previousState.equals(State.STATE_WIFI_ADAPTER_RESETTING)) {

                    setCurrentState(State.STATE_WIFI_ADAPTER_STARTING);
                    enableWifiAdapter();
                }else {
//                    padoc.print("ERROR : unknown WifiDirectManager iteration #00");
                }

                break;
            case STATE_WIFI_ADAPTER_RESETTING:

                //This is an intermediary state, currentState should not be evaluated here

                break;
            case STATE_WIFI_ADAPTER_STARTING:

                //This is an intermediary state, currentState should not be evaluated here

                break;
            case STATE_WIFI_ADAPTER_ENABLED:

                if(this.previousState.equals(State.STATE_WIFI_ADAPTER_STARTING)){

                    padoc.onWifiAdapterEnabled();

                }else {
//                    padoc.print("ERROR : unknown WifiDirectManager iteration #01");
                }

                break;
        }
    }

    //COMMANDS

    public void enable(){
        this.iterate();
    }

    private void enableWifiAdapter(){
        if(!wifiManager.setWifiEnabled(true)) {
//            padoc.print("ERROR : Could not enable WiFi.");
        }
    }

    private void stopWifiAdapter(){
        if(!wifiManager.setWifiEnabled(false)){
//            padoc.print("ERROR : Could not turn off WiFi.");
        }
    }

    public void resetWiFi(){
        if (wifiManager.isWifiEnabled()){
            setCurrentState(State.STATE_WIFI_ADAPTER_RESETTING);
            stopWifiAdapter();
        }else {
            setCurrentState(State.STATE_WIFI_ADAPTER_STARTING);
            wifiManager.setWifiEnabled(true);
        }
    }

    //=====WIFI CONNECTION

    public void connectHack(final String ssid, final String pass){

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = String.format("\"%s\"", ssid);
        conf.preSharedKey = String.format("\"%s\"", pass);

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        int netID = wifiManager.addNetwork(conf);

        wifiManager.disconnect();
        wifiManager.enableNetwork(netID, true);
        wifiManager.reconnect();
//        wifiManager.saveConfiguration();

//        //TODO : REMOVE THIS UGLY HACK, PROBLEM COMES BECAUSE WE DID DISCOVERY BEFORE
//        getActivity().startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), WIFI_SETTINGS_REQUEST_CODE);
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        getActivity().finishActivity(WIFI_SETTINGS_REQUEST_CODE);

    }

    //STATES

    private void setCurrentState(State currentState){
        this.previousState = this.currentState;
        this.currentState = currentState;
    }

    public State getCurrentState(){
        return this.currentState;
    }
}
