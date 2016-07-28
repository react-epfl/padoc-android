package com.react.gabriel.wbam.padoc.services;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.text.Html;

import com.react.gabriel.wbam.padoc.Padoc;

/**
 * Created by gabriel on 28/06/16.
 */
public class ServiceManager {

    public enum State {
        STATE_NULL,
        STATE_SERVICE_ADDED,
        STATE_DISCOVERY_RUNNING,
        STATE_SERVICE_ADDED_DISCOVERY_RUNNING,
    }

    private State currentState;
    private State previousState;

    private final Padoc padoc;
    private final Context context;

    private final ServiceAdvertiser serviceAdvertiser;
    private final ServiceDiscovery serviceDiscovery;

    public ServiceManager(Padoc padoc){

        this.padoc = padoc;
        this.context = padoc.getContext();

        this.currentState = State.STATE_NULL;
        this.previousState = State.STATE_NULL;

        this.serviceAdvertiser = new ServiceAdvertiser(padoc);
        this.serviceDiscovery = new ServiceDiscovery(padoc);
    }

    //LIFECYCLE

    private void iterate(){
        switch (this.currentState){
            case STATE_NULL:

                WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        setCurrentState(State.STATE_SERVICE_ADDED);
//                        padoc.print(Html.fromHtml("Service added..."), true);
                        iterate();
//                        padoc.connect();
                    }

                    @Override
                    public void onFailure(int reason) {
                        padoc.print(Html.fromHtml("ERROR : could not register service"), true);
                    }
                };

                this.serviceAdvertiser.startService(actionListener);

                break;
            case STATE_SERVICE_ADDED:

                if (this.previousState.equals(State.STATE_NULL)) {

                    this.serviceDiscovery.startDiscovery();

                    //TODO : Maybe need to confirm the discovery?
                    setCurrentState(State.STATE_SERVICE_ADDED_DISCOVERY_RUNNING);

                    padoc.onServiceAddedAndDiscoveryStarted();
                }else {
                    padoc.print(Html.fromHtml("ERROR : unknown WifiDirectManager iteration #02"), true);
                }

                break;
            case STATE_DISCOVERY_RUNNING:

                //TODO

//                padoc.print("ERROR : SHould not be here 00");

                break;
            case STATE_SERVICE_ADDED_DISCOVERY_RUNNING:

//                padoc.print("ERROR : SHould not be here 01");

                break;
        }
    }

    //COMMANDS

    public void start(){
        this.iterate();
    }

    public void stop(WifiP2pManager.ActionListener actionListener){
        this.stopDiscovery();
        this.stopService(actionListener);
    }

    public void stopDiscovery() {

        if (this.currentState.equals(State.STATE_DISCOVERY_RUNNING)) {
            this.serviceDiscovery.stopDiscovery(null);
            this.setCurrentState(State.STATE_NULL);
        }else if (this.currentState.equals(State.STATE_SERVICE_ADDED_DISCOVERY_RUNNING)) {
            this.serviceDiscovery.stopDiscovery(null);
            this.setCurrentState(State.STATE_SERVICE_ADDED);
        }else {
//            padoc.print("ERROR : Cannot stop a discovery that is not running");
        }
    }

    public void stopDiscoveryRaw(){
        this.serviceDiscovery.startDiscovery();
    }

    public void startDiscoveryRaw(){
        this.serviceDiscovery.startDiscovery();
    }

    public void stopService(WifiP2pManager.ActionListener actionListener){

        if(this.currentState.equals(State.STATE_SERVICE_ADDED)){
            this.serviceAdvertiser.stopService(actionListener);
            this.setCurrentState(State.STATE_NULL);
        } else if (this.currentState.equals(State.STATE_SERVICE_ADDED_DISCOVERY_RUNNING)) {
            this.serviceAdvertiser.stopService(actionListener);
            this.setCurrentState(State.STATE_DISCOVERY_RUNNING);
        }else {
//            padoc.print("ERROR : Cannot stop a service that is not being advertised");
        }
    }

    //STATE

    private void setCurrentState(State currentState){
        this.previousState = this.currentState;
        this.currentState = currentState;
    }

    public State getCurrentState(){
        return this.currentState;
    }
}