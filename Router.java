package com.react.gabriel.wbam.padoc;

import android.util.Pair;

import com.react.gabriel.wbam.padoc.connection.ConnectedThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 18/05/16.
 */
public class Router {

    public final int ADDRESS_UNKNOWN = -1;

    //connectedDevices<macAddress, connectedThread> These are the devices directly connected to us
    private Map<String, ConnectedThread> connectedDevices = null;
    //route<destinationAddress, <hops, routingAddress>>
    private Map<String, Pair<Integer, String>> route = null;
    //peerNames<address, name>
    private Map<String, String> peerNames = new HashMap<String, String>();
    //peerNames<address, hops>
    private Map<String, Integer> peerHops = new HashMap<String, Integer>();

    public Router(){

        this.connectedDevices = new HashMap<String, ConnectedThread>();
        this.route = new HashMap<String, Pair<Integer, String>>();

    }

//    public boolean threadIsConsideredOrphan(ConnectedThread thread){
//        return orphanDevices.contains(thread);
//    }

    public void createNewEntry(ConnectedThread connectedThread, String remoteAddress, String name){

        connectedThread.setRemoteAddress(remoteAddress);
        connectedDevices.put(remoteAddress, connectedThread);
        setRoute(name, remoteAddress, 0, remoteAddress);

    }

//    public boolean setOrphanThread(ConnectedThread connectedThread) {
//        return orphanDevices.add(connectedThread);
//    }

    public boolean knows(String remoteAddress){

        return route.containsKey(remoteAddress);
    }

    public int getHopsFor(String remoteAddress){
        if(route.containsKey(remoteAddress)){
            return route.get(remoteAddress).first;
        }else {
            return ADDRESS_UNKNOWN;
        }
    }

    public Pair<Integer, String> setRoute(String name, String destinationAddress, int hops, String routeAddress){

        peerNames.put(destinationAddress, name);
        peerHops.put(destinationAddress, hops);
        return route.put(destinationAddress, new Pair<Integer, String>(hops, routeAddress));
    }

    public ConnectedThread getRoutingThreadFor(String address){

        String routingAddress = route.get(address).second;
        return connectedDevices.get(routingAddress);
    }

    public ConnectedThread setConnectedDevice(String name, String remoteAddress, ConnectedThread connectedThread){

        setRoute(name, remoteAddress, 0, remoteAddress);
        return connectedDevices.put(remoteAddress, connectedThread);
    }

    public Set<Map.Entry<String, ConnectedThread>> getConnectedEntries(){
        return connectedDevices.entrySet();
    }

    public Collection<ConnectedThread> getConnectedThreads(){
        return connectedDevices.values();
    }

    public Set<Map.Entry<String, Pair<Integer, String>>> getKnownPeers(){
        return route.entrySet();
    }

    public Set<String> getPeers(){
        return route.keySet();
    }

    public String getNameFor(String address){
        return peerNames.get(address);
    }

    public Set<java.util.Map.Entry<String, String>> getPeerNamesAndHops(){

        return peerNames.entrySet();

//        String[] array = new String[peerNames.values().size()];
//
//        return peerNames.values().toArray(array);
    }

    public String getRoutingAddressFor(String address){
        return route.get(address).second;
    }

    public int numberOfActiveConnections(){
        return connectedDevices.size();
    }

    public boolean isConnectedTo(String btAddress){
        return connectedDevices.containsKey(btAddress);
    }

}