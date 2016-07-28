package com.react.gabriel.wbam.padoc.routing;

import android.text.Html;

import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.connection.BluetoothConnectedThread;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;
import com.react.gabriel.wbam.padoc.messaging.MessageManager;
import com.react.gabriel.wbam.padoc.connection.WifiConnectedThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 05/07/16.
 */
public class ParentRouter implements Router {

    public final int ADDRESS_UNKNOWN = -1;

    private Padoc padoc;

    //connectedParents<address, bluetoothConnectedThread>
    private Map<String, BluetoothConnectedThread> siblings;
    //connectedLeafs<address, wifiConnectedThread>
    private Map<String, WifiConnectedThread> leafs;

    //routes<peerAddress, Set<possibleRoutes>>
    private Map<String, Set<String>> routes = null;

    //hops<peerAddress@routingAddress, hops>
    private Map<String, Integer> hops = new HashMap<String, Integer>();

    //shortestRoute<peerAddress, shortestRoutingAddress>
    private Map<String, String> shortestRoute = new HashMap<String, String>();


    //peers<peerAddress, peerName>
    private Map<String, String> peers = new HashMap<String, String>();

    public ParentRouter(Padoc padoc){

        this.padoc = padoc;
        this.leafs = new HashMap<String, WifiConnectedThread>();
        this.siblings = new HashMap<String, BluetoothConnectedThread>();
        this.routes = new HashMap<String, Set<String>>();
        this.hops = new HashMap<String, Integer>();
    }

    public void handleNewLeaf(WifiConnectedThread leafThread){
        if(this.leafs.put(leafThread.getRemoteAddress(), leafThread) == null){

        }else{
//            this.padoc.print("ERROR : leaf was already present!");
        }
        this.setRoute(leafThread.getRemoteAddress(), leafThread.getRemoteAddress(), leafThread.getRemoteName(), 0);
        //TODO : send new leaf ID to all connections
    }

    public void handleNewSibling(BluetoothConnectedThread siblingThread){
        if(this.siblings.put(siblingThread.getRemoteAddress(), siblingThread) == null){

        }else {
//            this.padoc.print("ERROR : sibling was already present!");
        }
        this.setRoute(siblingThread.getRemoteAddress(), siblingThread.getRemoteAddress(), siblingThread.getRemoteName(), 0);
        //TODO : send new sibling ID to all connections
    }

    @Override
    public void handleNewConnection(ConnectedThread connectedThread) {
        if(connectedThread instanceof WifiConnectedThread){
            this.handleNewLeaf((WifiConnectedThread)connectedThread);
        }else {
            this.handleNewSibling((BluetoothConnectedThread)connectedThread);
        }
        this.padoc.print(Html.fromHtml("<b>New peer(s) available!</b>"), true);
    }

    public void setRoute(String destinationAddress, String routingAddress, String name, int hops){

        String hopsKey = destinationAddress+"@"+routingAddress;

        if(this.routes.containsKey(destinationAddress)){
            //We already know this peer, we have at least one routing addres for it.

            Set<String> gateways = routes.get(destinationAddress);

            if(gateways.contains(routingAddress) && this.hops.get(hopsKey) > hops){
                //This specific gateway is already known but it has been updated with less hops.

                this.hops.put(hopsKey, hops);

                if(hopsForShortestRouteTo(destinationAddress) > hops){
                    //This new route is actually shorter than the previous shortest one.

                    //Update the shortest route
                    this.shortestRoute.put(destinationAddress, routingAddress);

                }
            }else if(!gateways.contains(routingAddress)){
                //We now have a new possible gateway for this destination

                gateways.add(routingAddress);
                this.hops.put(hopsKey, hops);

                if(hopsForShortestRouteTo(destinationAddress) > hops){
                    //This new gateway is actually the shortest one

                    //Update the shortest route
                    this.shortestRoute.put(destinationAddress, routingAddress);
                }

            }
        }else {
            //This is a new Peer, hence a new route

            Set<String> routingSet = new HashSet<String>();
            routingSet.add(routingAddress);

            routes.put(destinationAddress, routingSet);
            shortestRoute.put(destinationAddress, routingAddress);
            this.hops.put(hopsKey, hops);
            peers.put(destinationAddress, name);

        }
    }

    public int hopsForShortestRouteTo(String destinationAddress){
        return hops.get(destinationAddress+"@"+shortestRoute.get(destinationAddress));
    }

    public int getHopsFor(String remoteAddress){

        if(routes.containsKey(remoteAddress)){
            return hopsForShortestRouteTo(remoteAddress);
        }else {
            return ADDRESS_UNKNOWN;
        }
    }

    public String getRoutingAddressFor(String address){
        return shortestRoute.get(address);
    }

    public ConnectedThread getRoutingThreadFor(String address){
        String shortestRoute = this.shortestRoute.get(address);

        if(leafs.containsKey(shortestRoute)){
            return leafs.get(shortestRoute);
        }else {
            return siblings.get(shortestRoute);
        }
    }

    public Set<java.util.Map.Entry<String, String>> getPeers(){

        return peers.entrySet();
    }

    public Set<String> getPeerAddresses(){
        return routes.keySet();
    }

    public String getPeerName(String address){
        if(address.equals(MessageManager.MULTICAST)){
            return MessageManager.MULTICAST;
        }
        return peers.get(address);
    }

    @Override
    public boolean isConnectedTo(String remoteAddress) {
        return (leafs.containsKey(remoteAddress) || siblings.containsKey(remoteAddress)) ? true : false;
    }

    @Override
    public boolean knows(String remoteAddress){
        return this.routes.containsKey(remoteAddress);
    }

    @Override
    public int getNumberOfWifiConnections() {
        return this.leafs.size();
    }

    @Override
    public int getNumberOfBluetoothConnections() {
        return this.siblings.size();
    }

    @Override
    public boolean hasLeafs(){
        return this.leafs.size() > 0 ? true : false;
    }

    @Override
    public boolean hasSiblings(){
        return this.siblings.size() > 0 ? true : false;
    }

    @Override
    public Set<Map.Entry<String, BluetoothConnectedThread>> getConnectedSiblings(){
        return this.siblings.entrySet();
    }

    @Override
    public Collection<BluetoothConnectedThread> getConnectedSiblingsThreads(){
        return this.siblings.values();
    }

    @Override
    public Set<Map.Entry<String, WifiConnectedThread>> getConnectedLeafs(){
        return this.leafs.entrySet();
    }
}
