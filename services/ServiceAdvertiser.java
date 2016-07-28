package com.react.gabriel.wbam.padoc.services;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.text.Html;

import com.react.gabriel.wbam.R;
import com.react.gabriel.wbam.padoc.Padoc;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gabriel on 27/06/16.
 */
public class ServiceAdvertiser {

    private final Padoc padoc;
    private final Resources resources;
    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;

    public ServiceAdvertiser(Padoc padoc){

        this.padoc = padoc;
        this.resources = padoc.getContext().getResources();
        this.mManager = (WifiP2pManager) padoc.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        this.mChannel = mManager.initialize(padoc.getContext(), padoc.getContext().getMainLooper(), null);

    }

    /**
     * When specifying custom actionListener, do not forget to set serviceIsRunning
     * @param actionListener
     */
    public void startService(WifiP2pManager.ActionListener actionListener) {
        Map<String, String> values = new HashMap<String, String>();
        //http://files.dns-sd.org/draft-cheshire-dnsext-dns-sd.txt | Sections 6.5 & 6.6
        //values cannot contain null values

        String meshUUID = this.padoc.getMeshUUID();
        String randomValue = this.padoc.getRandomValue().toString();

        if(meshUUID != null) values.put(this.resources.getString(R.string.MESH_UUID_VALUE), meshUUID);
        if(randomValue != null) values.put(this.resources.getString(R.string.RANDOM_VALUE), randomValue);

        if(this.padoc.getCurrentState().equals(Padoc.State.ONLINE_GO)){

            if(this.padoc.isAcceptingWifiConnections()){

                String ssid = this.padoc.getSSID();
                String pass = this.padoc.getPass();

                if(ssid != null) values.put(this.resources.getString(R.string.SSID_VALUE), ssid);
                if (pass != null) values.put(this.resources.getString(R.string.PASS_VALUE), pass);
            }

//            if(this.padoc.isAcceptingBluetoothConnections()){ This line only works with Android 5.0 and above. In KitKat you cannot have so many values, apparently
            if(this.padoc.isAcceptingBluetoothConnections() && !this.padoc.isAcceptingWifiConnections()){

                String bluetoothMac = this.padoc.getBluetoothMac();

                if(bluetoothMac != null) values.put(this.resources.getString(R.string.BTMAC_VALUE), bluetoothMac);
            }
        }

//        padoc.print("There are " + values.toString().getBytes().length + " bytes in this service:");
//        padoc.print(Html.fromHtml(values.toString()), true);

        WifiP2pServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("PADOC", "_http._tcp", values);

        mManager.addLocalService(mChannel, serviceInfo, actionListener);
    }

    /**
     * When using a custom actionListener do not forget to set serviceIsRunning
     * @param actionListener
     */
    public void stopService(WifiP2pManager.ActionListener actionListener) {
        mManager.clearLocalServices(mChannel, actionListener);
    }

    public void restart(final String deviceName){
        mManager.clearLocalServices(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                startService(null);
            }

            public void onFailure(int reason) {
            }
        });
    }
}
