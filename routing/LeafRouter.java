package com.react.gabriel.wbam.padoc.routing;

import android.text.Html;

import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.connection.BluetoothConnectedThread;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;
import com.react.gabriel.wbam.padoc.messaging.MessageManager;
import com.react.gabriel.wbam.padoc.connection.WifiConnectedThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 05/07/16.
 */
public class LeafRouter implements Router {

    public final int ADDRESS_UNKNOWN = -1;

    private Padoc padoc;

    //The only real connection this peer has
    private WifiConnectedThread wifiConnectedThread;

    private String gateway;

    //names<address, name>;
    private Map<String, String> names;
    //hops<address, hops>
    private Map<String, Integer> hops;

    public LeafRouter(Padoc padoc){
        this.padoc = padoc;
        this.names = new HashMap<String, String>();
        this.hops = new HashMap<String, Integer>();
    }

    @Override
    public void handleNewConnection(ConnectedThread connectedThread){
        if(this.wifiConnectedThread == null){
            this.wifiConnectedThread = (WifiConnectedThread) connectedThread;
            this.gateway = connectedThread.getRemoteAddress();
            this.setRoute(this.gateway, this.gateway, connectedThread.getRemoteName(), 0);
        }else {
//            this.padoc.print("ERROR : we already have a Connection!");
        }
        this.padoc.print(Html.fromHtml("<b>New peer(s) available!</b>"), true);
    }

    //Equivalent of setRoute in ParentRouter
    public void setRoute(String destinationAddress, String routingAddress, String name, int hops){
//        padoc.print("Setting route for : " + name);

        if(routingAddress.equals(this.gateway)){
            this.names.put(destinationAddress, name);
            this.hops.put(destinationAddress, hops);
        }else {
//            this.padoc.print("ERROR : routingAddress != gateway!");
//            this.padoc.print(routingAddress + " != " + this.gateway);
        }
    }

    @Override
    public int getHopsFor(String remoteAddress){

        if(this.hops.containsKey(remoteAddress)){
            return hops.get(remoteAddress);
        }else {
            return ADDRESS_UNKNOWN;
        }
    }

    @Override
    public ConnectedThread getRoutingThreadFor(String address) {
        return this.wifiConnectedThread;
    }

    @Override
    public boolean knows(String remoteAddress){
        return this.names.containsKey(remoteAddress);
    }

    @Override
    public Set<Map.Entry<String, String>> getPeers(){
        return names.entrySet();
    }

    @Override
    public boolean isConnectedTo(String remoteAddress){
//        this.padoc.print("ERROR : Should not be used in Leaf! 06");
        return remoteAddress.equals(this.wifiConnectedThread.getRemoteAddress());
    }

    @Override
    public int getNumberOfWifiConnections() {
//        this.padoc.print("ERROR : Should not be used in Leaf! 05");
        return this.wifiConnectedThread == null ? 0 : 1;
    }

    @Override
    public int getNumberOfBluetoothConnections() {
//        this.padoc.print("ERROR : Should not be used in Leaf! 04");
        return 0;
    }

    @Override
    public Set<String> getPeerAddresses() {
//        this.padoc.print("ERROR : Should not be used in Leaf! 00");
        return null;
    }

    @Override
    public String getPeerName(String address) {
        if(address.equals(MessageManager.MULTICAST)){
            return MessageManager.MULTICAST;
        }
        return this.names.get(address);
    }

    @Override
    public String getRoutingAddressFor(String address) {
//        this.padoc.print("ERROR : Should not be used in Leaf! 02");
        return null;
    }

    @Override
    public boolean hasLeafs(){
//        this.padoc.print("ERROR : Should not be used in Leaf! 07");
        return false;
    }

    @Override
    public boolean hasSiblings(){
//        this.padoc.print("ERROR : Should not be used in Leaf! 08");
        return false;
    }

    @Override
    public Set<Map.Entry<String, BluetoothConnectedThread>> getConnectedSiblings(){
//        this.padoc.print("ERROR : Should not be used in Leaf! 09");
        return null;
    }

    @Override
    public Collection<BluetoothConnectedThread> getConnectedSiblingsThreads() {
//        this.padoc.print("ERROR : Should not be used in Leaf! 11");
        return null;
    }

    @Override
    public Set<Map.Entry<String, WifiConnectedThread>> getConnectedLeafs(){
//        this.padoc.print("ERROR : Should not be used in Leaf! 10");
        return null;
    }
}