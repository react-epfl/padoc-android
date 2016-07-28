package com.react.gabriel.wbam.padoc.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.connection.WifiServerThread;

import java.util.Collection;

/**
 * Created by gabriel on 28/06/16.
 */
public class GroupManager extends BroadcastReceiver {

    private final Padoc padoc;
    private final Context context;
    private final Channel channel;

    private final WifiP2pManager p2pManager;
    private WifiP2pGroup p2pGroup = null;

    private boolean localAPisON = false;

    private WifiServerThread server;
    private boolean serverIsRunning = false;

    public GroupManager(Padoc padoc){

        this.padoc = padoc;
        this.context = padoc.getContext();
        this.p2pManager = (WifiP2pManager) this.context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.p2pManager.initialize(this.context, this.context.getMainLooper(), null);
        this.server = new WifiServerThread(padoc);
    }

    //BROADCAST RECEIVER

    public void onReceive(Context context, Intent intent){

        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            /*
             * Here the group info changed
             * Access to:
             * EXTRA_WIFI_P2P_INFO
             * EXTRA_NETWORK_INFO
             * EXTRA_WIFI_P2P_GROUP
             */

            WifiP2pGroup tempGroup = (WifiP2pGroup) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

            if (tempGroup != null && tempGroup.getNetworkName() != null){
                //Group info changed
                this.p2pGroup = tempGroup;

                //TODO : set SSID and passphrase if group is created for the first time.
//                Method setPassphraseMethod = null;
//                Method setNetworkNameMethod = null;
//
//                try {
//                    setPassphraseMethod = p2pGroup.getClass().getMethod("setPassphrase", new Class[]{String.class});
//                    setNetworkNameMethod = p2pGroup.getClass().getMethod("setNetworkName", new Class[]{String.class});
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                }
//
//                if(setNetworkNameMethod != null && setNetworkNameMethod != null){
//                    try {
//                        setNetworkNameMethod.invoke(p2pGroup, "PADOCSSID");
//                        setPassphraseMethod.invoke(p2pGroup, "PADOCPASSPHRASE");
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    } catch (InvocationTargetException e) {
//                        e.printStackTrace();
//                    }
//                }

                padoc.onP2PGroupChanged();

            }else {
                this.p2pGroup = null;
//                padoc.print("P2P group is empty");
            }

        }

        //TODO : This should be in MyWifiManager
        else if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){

            String networkName = intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO);

//            padoc.print("Connected to : " + networkName);

            if(!networkName.equals("<unknown ssid>")){

                //TODO : VERIFY ITS THE RIGHT SSID
                //TODO : Interrupt wifiConnectionTimeoutThread

                this.padoc.onWifiConnectionSucceeded();

            }
        }
    }

    //COMMANDS

    public void createGroup(){

        if (!this.localAPisON) {

            WifiP2pManager.ActionListener actionListener = new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    localAPisON = true;
                }

                @Override
                public void onFailure(int reason) {
                    localAPisON = false;
                }
            };

            this.p2pManager.createGroup(this.channel, actionListener);

        } else {
//            padoc.print("ERROR : a local group already exists.");
            padoc.onP2PGroupCreationFailed();
        }
    }

    public void removeGroup(){
        if(this.localAPisON){
            this.p2pManager.removeGroup(this.channel, null);
        }
    }

    //GETTERS

    public String getSSID(){
        return p2pGroup != null ? p2pGroup.getNetworkName() : null;
    }

    public String getPass(){
        return p2pGroup != null ? p2pGroup.getPassphrase() : null;
    }

    public Collection<WifiP2pDevice> getClients(){
        return p2pGroup != null ? p2pGroup.getClientList() : null;
    }

    //VERIFIERS

    public boolean isGroupOwner(){
        return p2pGroup != null ? p2pGroup.isGroupOwner() : false;
    }

    public boolean isGroupActive(){
        return this.p2pGroup != null;
    }

    public boolean isServerRunning(){
        return this.serverIsRunning;
    }

    public void startServer(){
        this.server.start();
    }

    public void stopServer(){
        this.server.interrupt();
    }
}
