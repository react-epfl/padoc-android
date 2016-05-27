package com.react.gabriel.wbam.padoc;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Pair;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.bluetooth.BluetoothManager;
import com.react.gabriel.wbam.padoc.wifidirect.WifiDirectManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by gabriel on 25/05/16.
 */
public class PadocManager {

    private String ALL = "ALL";

    private boolean DBG = true;
    private MainActivity mActivity;

    private String localBluetoothAddress;
    private String localWDAddress;

    private Router mRouter;
    private Messenger mMessenger;

    //Bluetooth
    private BluetoothManager btManager;
    private IntentFilter btIntentFilter;

    //WifiDirect
    private WifiDirectManager wdManager;
    private IntentFilter wdIntentFilter;

    //Set containing addresses running PADOC
    private Set<String> padocReadyDevices = new HashSet<String>();

    //Set containing peers addresses (first) and names (second) in the network
    private Set<Pair<String, String>> padocPeers = new HashSet<Pair<String, String>>();

    public PadocManager(MainActivity mActivity) {

        this.mActivity = mActivity;

        //Bluetooth
        btManager = new BluetoothManager(mActivity, this);
        this.localBluetoothAddress = btManager.getLocalBluetoothAddress();

        //Router
        this.mRouter = new Router(localBluetoothAddress);
        btManager.setRouter(mRouter);

        //Messenger
        this.mMessenger = new Messenger(mActivity, mRouter, localBluetoothAddress);
        btManager.setMessenger(mMessenger);

        btIntentFilter = new IntentFilter();
        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        mActivity.registerReceiver(btManager, btIntentFilter);

        //WifiDirect
        wdManager = new WifiDirectManager(mActivity, this);

        wdIntentFilter = new IntentFilter();
        wdIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wdIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mActivity.registerReceiver(wdManager, wdIntentFilter);

    }

    //Bluetooth functions

    public void startBluetoothDiscovery(){
        btManager.startDiscovery();
    }

    public void stopBluetoothDiscovery(){
        btManager.stopDiscovery();
    }

    public void setBluetoothVisible(){
        btManager.setVisible();
    }

    public void setBluetoothInvisible(){
        btManager.setInvisible();
    }

    public void startBluetoothServer(){
        btManager.startServer();
    }

    public void stopBluetoothServer(){
        btManager.stopServer();
    }

    public void unpairBluetoothDevices(){
        btManager.unpairDevices();
    }

    public String getLocalBluetoothAddress(){
        return localBluetoothAddress;
    }

    //WifiDirect functions

    public void startWifiDirectService(){
        wdManager.startService();
    }

    public void stopWifiDirectService(){
        wdManager.stopService();
    }

    public void startWifiDirectDiscovery(){
        wdManager.startDiscovery();
    }

    public void stopWifiDirectDiscovery(){
        wdManager.stopDiscovery();
    }

    public void registerNewBluetoothAddress(String btAddress){
        padocReadyDevices.add(btAddress);
        //TODO : if this is the first addition to the set, start bluetooth discovery.
    }

    //PADOC functions

    public boolean verifyPadocAddress(String address){
        return padocReadyDevices.contains(address);
    }

    public String[] getPeers(){

        Set<String> peers = mRouter.getPeers();

        String[] array = new String[peers.size()];

        return peers.toArray(array);
    }

    public void sendMsg(String address){
        String msg = "Hello World";
        mMessenger.sendMsg(msg, address);
    }

    //Debug functions
    public void debugPrint(String msg){
//        if (DBG) System.out.println(msg);
        mActivity.debugPrint(msg);
    }

    //Temporary functions

    public void sendCBS(){
        mMessenger.sendMsg("Hallo CBS", ALL);
    }
}
