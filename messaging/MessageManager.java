package com.react.gabriel.wbam.padoc.messaging;

import android.text.Html;

import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.connection.BluetoothConnectedThread;
import com.react.gabriel.wbam.padoc.connection.ConnectedThread;
import com.react.gabriel.wbam.padoc.routing.Router;
import com.react.gabriel.wbam.padoc.messaging.multicast.MulticastSentListener;
import com.react.gabriel.wbam.padoc.messaging.multicast.SendMulticastAsyncTask;
import com.react.gabriel.wbam.padoc.connection.WifiConnectedThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 07/07/16.
 */
public class MessageManager implements MulticastSentListener{

    public static final String MULTICAST = "multicast";

    private Padoc padoc;
    private Router router;

    //Map<UUID, Set<Sources>>
    private Map<String, Set<String>> cbsMsgSourceTracker = new HashMap<String, Set<String>>();
    private Map<String, Integer> cbsMsgMulticastTracker = new HashMap<String, Integer>();

    //transmissions
    private int transmissions = 0;

    //messagesFromPeer<messagesFromPeer, Set<messageUUID>>
    private Map<String, Set<String>> messagesFromPeer = new HashMap<String, Set<String>>();

    //messageCount<msgUUID, retransmissions>
    private Map<String, Integer> messageCount = new HashMap<String, Integer>();

    private Set<String> floodMsgTracker = new HashSet<String>();

    public MessageManager(Padoc padoc, Router router){

        this.padoc = padoc;
        this.router = router;
    }
    /**
     * Decides what to do with any received message,
     * either prints the message if it is meant for this device and/or forwards it otherwise.
     * @param message
     * @param fromThread
     */
    public void handleMessage(Message message, ConnectedThread fromThread){

        Message.Algo algo = message.getAlgo();

        Message.Type contentType = message.getType();

        final String sourceAddress = message.getSource();

        String destination = message.getDestination();
        String msgID;

        switch (algo){

            case FLOOD:
                //here, fromThread is null if the received message is a Multicast

                String newAddress = null;
                String newName = null;
                int hops;

                switch (contentType) {
                    case IDS:
//                        this.padoc.print("Got IDS");

                        String newAddresses = message.getMsg();

                        int n = 0;

                        for (String address : newAddresses.split(">")) {

                            String[] newAddressInfo = address.split("<");

                            String[] newMacAndName = newAddressInfo[0].split("-");
                            newAddress = newMacAndName[0];
                            newName = newMacAndName[1];
                            hops = Integer.parseInt(newAddressInfo[1]);

                            if (!this.router.knows(newAddress) || (this.router.knows(newAddress) && this.router.getHopsFor(newAddress) > hops)) {
                                this.router.setRoute(newAddress, sourceAddress, newName, hops);
                                n++;
                            }
                        }

//                        if (n > 0) this.padoc.print("Saved " + n + " new addresses");

                        if(message.getDestination().equals(Message.ALL)){
                            //TODO
//                            forwardBroadcastFLOODMsg(message, fromThread.getRemoteAddress());
                        }

                        break;
                    case MSG:

                        if(messagesFromPeer.containsKey(sourceAddress)){
                            Set<String> messagesUUID = messagesFromPeer.get(sourceAddress);
                            if(!messagesUUID.contains(message.getUUID())){
                                messagesUUID.add(message.getUUID());
                            }
                        }else{
                            Set<String> messageUUID = new HashSet<>();
                            messageUUID.add(message.getUUID());
                            messagesFromPeer.put(sourceAddress, messageUUID);
                        }

                        msgID = message.getUUID();

                        if(!floodMsgTracker.contains(msgID)){
                            //This is a brand new flooding msg. yay.

                            floodMsgTracker.add(msgID);

                            String fromAddress;

                            if(fromThread == null){
                                //we got this from a multicast
                                fromAddress = MULTICAST;

                            }else{
                                //we got this from a Bluetooth connection
                                fromAddress = fromThread.getRemoteAddress();

                            }

                            if(destination.equals(Message.ALL) || destination.equals(this.padoc.getBluetoothMac())){

                                printMsg(message, fromAddress);
                            }

                            forwardFLOOD(message, fromAddress);

                        }else {
                            //TODO reset timer for tracking deletion
                        }

                        break;
                    case FLOOD_TEST_REQUEST:

                        msgID = message.getUUID();

                        if(!floodMsgTracker.contains(msgID)){

                            floodMsgTracker.add(msgID);

                            final String floodRequestInterval = message.getMsg();

                            this.padoc.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    TestThread testThread = new TestThread(padoc);
//                                    padoc.print("STARTING FLOOD at interval : " + floodRequestInterval);
                                    testThread.startTest(Message.Algo.FLOOD, Integer.valueOf(floodRequestInterval), null);
                                }
                            });

                            forwardFLOOD(message, null);
                        }

                        break;
                    case CBS_TEST_REQUEST:

                        msgID = message.getUUID();

                        if(!floodMsgTracker.contains(msgID)){

                            floodMsgTracker.add(msgID);

                            final String cbsRequestInterval = message.getMsg();

                            this.padoc.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    TestThread testThread = new TestThread(padoc);
//                                    padoc.print("STARTING CBS at interval : " + cbsRequestInterval);
                                    testThread.startTest(Message.Algo.CBS, Integer.valueOf(cbsRequestInterval), null);
                                }
                            });

                            forwardFLOOD(message, null);
                        }

                        break;
                    case ROUTE_TEST_REQUEST:

                        msgID = message.getUUID();

                        if(!floodMsgTracker.contains(msgID)){

                            floodMsgTracker.add(msgID);

                            final String routeRequestInterval = message.getMsg();

                            this.padoc.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    TestThread testThread = new TestThread(padoc);
//                                    padoc.print("STARTING ROUTE at interval : " + routeRequestInterval);
                                    testThread.startTest(Message.Algo.ROUTE, Integer.valueOf(routeRequestInterval), sourceAddress);
                                }
                            });

                            forwardFLOOD(message, null);
                        }

                        break;
                }
                break;
            case CBS:
                //here, fromThread is null if the received message is a Multicast

                switch (contentType){
                    case MSG:

                        if(messagesFromPeer.containsKey(sourceAddress)){
                            Set<String> messagesUUID = messagesFromPeer.get(sourceAddress);
                            if(!messagesUUID.contains(message.getUUID())){
                                messagesUUID.add(message.getUUID());
                            }
                        }else{
                            Set<String> messageUUID = new HashSet<>();
                            messageUUID.add(message.getUUID());
                            messagesFromPeer.put(sourceAddress, messageUUID);
                        }

//                        this.padoc.print("We got a CBS");

                        msgID = message.getUUID();

                        if(this.padoc.isParent()){

                            if(fromThread == null){
                                //Comes from wifi multicast

                                if(cbsMsgMulticastTracker.containsKey(msgID)){
                                    //We are in RAD, msg has already been received through multicast, increment counter.

                                    cbsMsgMulticastTracker.put(msgID, cbsMsgMulticastTracker.get(msgID)+1);

                                }else if(!cbsMsgMulticastTracker.containsKey(msgID) && !cbsMsgSourceTracker.containsKey(msgID)){
                                    //first time we get this CBS message

                                    String msgSource;
                                    msgSource = MULTICAST;
                                    cbsMsgMulticastTracker.put(msgID, 1);

                                    new CBSThread(this.padoc, this, message).start();

                                    if(destination.equals(this.padoc.getBluetoothMac()) || destination.equals(Message.ALL)){
                                        this.printMsg(message, MULTICAST);
                                    }
                                }

                            }else {
                                //Comes from bluetooth

                                if(cbsMsgSourceTracker.containsKey(msgID)){
                                    //We are in RAD, msg has already been received through bluetooth, increment counter.

                                    Set previousSources = cbsMsgSourceTracker.get(msgID);

                                    String newSource = fromThread.getRemoteAddress();

                                    if(!previousSources.contains(newSource)){
                                        previousSources.add(newSource);
                                        cbsMsgSourceTracker.put(msgID, previousSources);
                                    }else {
//                                        this.padoc.print("ERROR : Got duplicate CBS from same source : " + this.router.getPeerName(newSource));
                                    }
                                }else if(!cbsMsgMulticastTracker.containsKey(msgID) && !cbsMsgSourceTracker.containsKey(msgID)){
                                    //first time we get this CBS message

                                    String msgRoute = fromThread.getRemoteAddress();
                                    Set source = new HashSet();
                                    source.add(msgRoute);
                                    cbsMsgSourceTracker.put(msgID, source);

                                    new CBSThread(this.padoc, this, message).start();

                                    if(destination.equals(this.padoc.getBluetoothMac()) || destination.equals(Message.ALL)){
                                        this.printMsg(message, msgRoute);
                                    }

                                }
                            }
                        }else {

//                            this.padoc.print("We are leaf");

                            if(!cbsMsgMulticastTracker.containsKey(msgID)){
                                //This is the first time we get this CBS
//                                this.padoc.print("First time!");

                                cbsMsgMulticastTracker.put(msgID, 1);

                                new CBSThread(this.padoc, this, message).start();

                                if(destination.equals(this.padoc.getBluetoothMac()) || destination.equals(Message.ALL)){
                                    this.printMsg(message, MULTICAST);
                                }
                            }else {
//                                this.padoc.print("N++");
                                //We are in RAD, msg has already been received through multicast, increment counter.

                                cbsMsgMulticastTracker.put(msgID, cbsMsgMulticastTracker.get(msgID)+1);
                            }

                        }
                        break;
                }

                break;
            case ROUTE:

                switch (contentType){
                    case ID_FWD:

                        newAddress = null;
                        newName = null;
                        hops = message.getHops();

                        try{
                            JSONObject jsonMessage = new JSONObject(message.getMsg());
                            newAddress = jsonMessage.getString(Message.ADDRESS);
                            newName = jsonMessage.getString(Message.NAME);
                        }catch (JSONException e){
                            e.printStackTrace();
                        }

                        if (newAddress == null || newName == null){
//                            this.padoc.print("ERROR : could not get message fields.");
                            break;
                        }

                        if(!this.router.knows(newAddress) || (this.router.knows(newAddress) && this.router.getHopsFor(newAddress) >= hops)){
                            //If we don't have this address registered yet, or if we do but this route is shorter, save it and broadcast.

                            //TODO : why not use sourceAddress instead of getRemoteAddress()
                            this.router.setRoute(newAddress, fromThread.getRemoteAddress(), newName, hops);
                            this.padoc.print(Html.fromHtml("<b>New peer(s) available!</b>"), true);

                            if (this.padoc.isParent()){
                                message.incrementHop();
                                this.singleBroadcast(message, fromThread.getRemoteAddress());
                            }

                        }

                        break;
                    case ACK_REQUEST:

                        break;

                    case ACK:

                        break;

                    case MSG:

                        if(messagesFromPeer.containsKey(sourceAddress)){
                            Set<String> messagesUUID = messagesFromPeer.get(sourceAddress);
                            if(!messagesUUID.contains(message.getUUID())){
                                messagesUUID.add(message.getUUID());
                            }
                        }else{
                            Set<String> messageUUID = new HashSet<>();
                            messageUUID.add(message.getUUID());
                            messagesFromPeer.put(sourceAddress, messageUUID);
                        }

                        if(destination.equals(this.padoc.getBluetoothMac())){
                            //Msg has reached its destination, display it.
//                            printMsg(message);
                            this.printMsg(message, fromThread.getRemoteAddress());

                        }else if(this.router.knows(destination)){
                            //This is not the final destination of the message, forward it.
                            forwardMsg(message);

                        }else if(destination.equals(Message.ALL)){
//                            this.padoc.print("ERROR : Got malformed message, ROUTE algorithm cannot be used to deliver to ALL");
                        }else {
//                            this.padoc.print("ERROR : forward destination unknown");
                        }

                        break;
                }

                break;
        }
    }

    public int getCBSSourceCountFor(String msgUUID){
        if(this.cbsMsgSourceTracker.containsKey(msgUUID)){
            return this.cbsMsgSourceTracker.get(msgUUID).size();
        }else {
            return 0;
        }
    }

    public int getCBSMulticastCountFor(String msgUUID){
        if(this.cbsMsgMulticastTracker.containsKey(msgUUID)){
            return this.cbsMsgMulticastTracker.get(msgUUID);
        }else {
            return 0;
        }
    }

    public Set<String> getCBSBannedListFor(String uuid){
        if(this.cbsMsgSourceTracker.containsKey(uuid)){
            return cbsMsgSourceTracker.get(uuid);
        }else {
            return new HashSet<String>();
        }
    }

    public void sendMeshInfoTo(String newAddress){
        String knownPeersString = "";

        int n = 0;
        for(String knownAddress : this.router.getPeerAddresses()){
            String knownAddressName = this.router.getPeerName(knownAddress);

            if(!knownAddress.equals(newAddress)){
                n++;
                int hops = this.router.getHopsFor(knownAddress)+1;
                knownPeersString += knownAddress+"-"+knownAddressName+"<"+hops+">";
            }
        }

        if(n > 0){
            Message knownPeersMessage = new Message(Message.Algo.FLOOD, Message.Type.IDS, knownPeersString, this.padoc.getBluetoothMac(), newAddress, 0);
//            this.padoc.print("Sending " + n + " known peers.");
            this.router.getRoutingThreadFor(newAddress).write(knownPeersMessage);
        }

    }

    public void forwardNewPeerIDToMesh(ConnectedThread connectedThread){

        Message fwdIDmsg = Message.getIDFWDMessage(this.padoc.getBluetoothMac(), connectedThread.getRemoteAddress(), connectedThread.getRemoteName());

        this.singleBroadcast(fwdIDmsg, connectedThread.getRemoteAddress());
    }

    public void singleBroadcast(Message message, String exceptAddress){

        if (this.router.hasLeafs()) {
            singleFloodToLeafs(message, exceptAddress);
        }

        if(this.router.hasSiblings()){
            this.singleFloodToSiblings(message, exceptAddress);
        }

    }

    public void singleFloodToLeafs(Message message, String exceptAddress){

        for(Map.Entry<String, WifiConnectedThread> connectedEntry : this.router.getConnectedLeafs()){
            if(!connectedEntry.getKey().equals(exceptAddress)){
                connectedEntry.getValue().write(message);
            }
        }
    }

    public void singleFloodToSiblings(Message message, String exceptAddress){

        for(Map.Entry<String, BluetoothConnectedThread> connectedEntry : this.router.getConnectedSiblings()){
            if(!connectedEntry.getKey().equals(exceptAddress)){
                connectedEntry.getValue().write(message);
            }
        }
    }

    public void forwardFLOOD(Message message, String fromAddress){

        String msgID = message.getUUID();

        if(messageCount.containsKey(msgID)){
            messageCount.put(msgID, messageCount.get(msgID)+1);
        }else {
            messageCount.put(msgID, 1);
        }

        message.incrementHop();

        sendMsg(message);
    }

    public void forwardMsg(Message message){

        String destination = message.getDestination();

        message.incrementHop();
        this.router.getRoutingThreadFor(destination).write(message);
    }

    public void forwardCBSToSiblings(Message message, Set<String> bannedAddresses){

//        this.padoc.print("forwarding cbs as");

        message.incrementHop();

        if(this.padoc.isParent() && this.router.hasSiblings()){
//            this.padoc.print("to siblings");

            for(Map.Entry<String, BluetoothConnectedThread> connectedEntry : this.router.getConnectedSiblings()){
                if(!bannedAddresses.contains(connectedEntry.getKey())){

//                    this.padoc.print("FWD");
                    String msgID = message.getUUID();

                    if(messageCount.containsKey(msgID)){
                        messageCount.put(msgID, messageCount.get(msgID)+1);
                    }else {
                        messageCount.put(msgID, 1);
                    }
                    connectedEntry.getValue().write(message);
                }
            }
        }
    }

    public void forwardMulticast(Message message){

        if(this.padoc.isParent() && this.router.hasLeafs() || !this.padoc.isParent()){
            message.incrementHop();

            String msgID = message.getUUID();

            if(messageCount.containsKey(msgID)){
                messageCount.put(msgID, messageCount.get(msgID)+1);
            }else {
                messageCount.put(msgID, 1);
            }

            new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();
        }
    }

    private void addEvaluationResults(Message message, String route){

    }

    public void clearMessageCount(){
        messageCount.clear();
        messagesFromPeer.clear();
        floodMsgTracker.clear();
        cbsMsgSourceTracker.clear();
        cbsMsgMulticastTracker.clear();
    }

    public void printMessageCount(){
        for(String address : messagesFromPeer.keySet()){
//            this.padoc.print(this.router.getPeerName(address) + " : " + messagesFromPeer.get(address).size()
//                    + " messages received (" + getMessagesRetranmissionForPeer(address) + ") retransmissions");
        }
    }

    private int getMessagesRetranmissionForPeer(String address){
        int n = 0;
        for(String messageUUID : messagesFromPeer.get(address)){
            if(messageCount.containsKey(messageUUID)){
                n += messageCount.get(messageUUID);
            }
        }

        return n;
    }

    public void sendMsg(Message message){

        Message.Algo algo = message.getAlgo();

        switch (algo){
            case FLOOD:
                if (!floodMsgTracker.contains(message.getUUID())) floodMsgTracker.add(message.getUUID());

                if(this.padoc.isParent()){
                    if(this.router.hasLeafs()){
                        //Multicast message
                        new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();
                    }

                    if(this.router.hasSiblings()){

                        for(BluetoothConnectedThread bluetoothConnectedThread : this.router.getConnectedSiblingsThreads()){
                            bluetoothConnectedThread.write(message);
                        }
                    }
                }else {
                    new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();
                }

                break;
        }
    }

    public void sendMsg(String msg, String destination, Message.Algo algo){

        Message message;

        switch (algo){
            case ROUTE:

                if(destination.equals(Message.ALL)){
//                    this.padoc.print("ERROR : Cannot send a ROUTE to ALL");
                }else if(this.router.knows(destination)){

                    message = new Message(algo, Message.Type.MSG, msg, this.padoc.getBluetoothMac(), destination, 0);
                    ConnectedThread routingThread = this.router.getRoutingThreadFor(destination);

                    routingThread.write(message);
//                    this.padoc.print("ME : " + msg);

                }else if(destination.equals(this.padoc.getBluetoothMac())){
//                    this.padoc.print("You cannot message yourself");
                }else {
//                    this.padoc.print("ERROR : Destination unknown");
                }

                break;
            case FLOOD:

                message = new Message(algo, Message.Type.MSG, msg, this.padoc.getBluetoothMac(), Message.ALL, 0);

                floodMsgTracker.add(message.getUUID());

                if(this.padoc.isParent()){
                    if(this.router.hasLeafs()){
                        //Multicast message
                        new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();
                    }

                    if(this.router.hasSiblings()){

                        for(BluetoothConnectedThread bluetoothConnectedThread : this.router.getConnectedSiblingsThreads()){
                            bluetoothConnectedThread.write(message);
                        }
                    }
                }else {
                    new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();
                }

                break;
            case CBS:

                message = new Message(algo, Message.Type.MSG, msg, this.padoc.getBluetoothMac(), Message.ALL, 0);

                if(this.padoc.isParent()){

                    if(this.router.hasLeafs()){

                        cbsMsgMulticastTracker.put(message.getUUID(), 1);

                        //Multicast message
                        new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();

                    }

                    if(this.router.hasSiblings()){

                        HashSet<String> localSourceSet = new HashSet<>();
                        localSourceSet.add(this.padoc.getBluetoothMac());
                        cbsMsgSourceTracker.put(message.getUUID(), localSourceSet);

                        for(BluetoothConnectedThread bluetoothConnectedThread : this.router.getConnectedSiblingsThreads()){
                            bluetoothConnectedThread.write(message);
                        }
                    }
                }else {

                    cbsMsgMulticastTracker.put(message.getUUID(), 1);
                    new SendMulticastAsyncTask(this, message.toString(), this.padoc.isParent()).execute();
                }
                break;
        }
    }

    public void requestTest(Message.Type type, Integer interval, String target){

        Message m = new Message(Message.Algo.FLOOD, type, interval.toString(), this.padoc.getBluetoothMac(), target, 0);

        this.sendMsg(m);
    }

    @Override
    public void onCouldNotSendMulticast() {
//        this.padoc.print("COULD NOT SEND MULTI");
    }

    private void printMsg(Message message, String route){

        String source = message.getSource();

        addEvaluationResults(message, route);

        switch (message.getAlgo()){
            case FLOOD:

                //Print FLOOD in Blue
                this.padoc.print(Html.fromHtml("<b><big><font color=\"#0095c2\">" + this.router.getPeerName(source) + ": " + message.getMsg() + "</font></b></big>"), true);

                break;
            case CBS:

                //Print CBS in Green
                this.padoc.print(Html.fromHtml("<b><big><font color=\"#008000\">" + this.router.getPeerName(source) + ": " + message.getMsg() + "</font></b></big>"), true);

                break;
            case ROUTE:

                this.padoc.print(Html.fromHtml(this.router.getPeerName(message.getSource()) + ": " + message.getMsg()), true);

                break;
        }
    }

    public String getColorFor(String macAddress) {

        float[] hsv = new float[3];
//        int color = getColor();
//        Color.colorToHSV(color, hsv);
//        hsv[2] *= 0.8f; // value component
//        color = Color.HSVToColor(hsv);

        String color = macAddress.replace(":", "");
        return color.substring(color.length() - 6);
    }

    public void incrementTransmissions(){
        this.transmissions++;
    }
}