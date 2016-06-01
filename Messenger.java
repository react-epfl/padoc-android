package com.react.gabriel.wbam.padoc;

import android.util.Pair;

import com.react.gabriel.wbam.MainActivity;
import com.react.gabriel.wbam.padoc.bluetooth.ConnectedThread;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by gabriel on 17/05/16.
 */
public class Messenger {

    private static final String ALL = "ALL";

    private MainActivity mActivity = null;
    private Router mRouter;
    private String localBluetoothAddress;

    private Map<String, Set<String>> cbsMsgTracker = new HashMap<String, Set<String>>();

    public Messenger(MainActivity mActivity, Router router, String localBluetoothAddress){

        this.mActivity = mActivity;
        this.mRouter = router;
        this.localBluetoothAddress = localBluetoothAddress;

    }

    /**
     * Decides what to do with any received message,
     * either prints the message if it is meant for this device and/or forwards it otherwise.
     * @param message
     * @param fromThread
     */
    public void deliverMsg(Message message, ConnectedThread fromThread){

        Message.Algo algo = message.getAlgo();

        Message.ContentType contentType;

        switch (algo){

            case CBS:

                String msgID = message.getUUID();

                if(!cbsMsgTracker.containsKey(msgID)){
                    //First time we get this msg, initialize counter to one.

                    mActivity.debugPrint("Got CBS");
                    Set sources = new HashSet();
                    sources.add(fromThread.getRemoteAddress());

                    cbsMsgTracker.put(msgID, sources);

                    new CBSThread(this, message).start();

                    printMsg(message);

                    //Initialize RAD
                }else{
                    //We are in RAD, msg has already been received, increment counter by one.
                    Set sources = cbsMsgTracker.get(msgID);
                    mActivity.debugPrint("Got CBS again");

                    if(!sources.contains(fromThread.getRemoteAddress())){
                        sources.add(fromThread.getRemoteAddress());
                        cbsMsgTracker.put(msgID, sources);
                    }else {
                        mActivity.debugPrint("ERROR : Got duplicate msg from same source");
                    }
                }

                break;

            case SINGLE:

                String destination = message.getDestination();

                if(destination.equals(localBluetoothAddress)){
                    //Msg has reached its destination, display it.
                    printMsg(message);
                }else{
                    //This is not the final destination of the message, forward it.
                    forwardMsg(message);
                }
                break;

            case IDProp:

                contentType = message.getContentType();

                switch (contentType){

                    case ID:

                        mActivity.debugPrint("Got ID");

                        String newAddress = message.getMsg();
                        String sourceAddress = message.getSource();

                        int hops = message.getHops();

                        if(fromThread.isOrphan() && hops == 0 && sourceAddress.equals(newAddress)){
                            //ID msg is original (not a forward)

                            mRouter.identifyOrphanThread(fromThread, newAddress);
                            mActivity.debugPrint("Saved original ID");

                            //Because the peer is new we should greet him with the necessary info
                            sendMeshInfoTo(newAddress);

                            if(message.getDestination().equals(ALL)) {
                                forwardBroadcastIDMsg(message, fromThread.getRemoteAddress());
                            }
                        }

                        if(!fromThread.isOrphan() && (!mRouter.knows(newAddress) || (mRouter.knows(newAddress) && mRouter.getHopsFor(newAddress) > hops))){

                            //If we don't have this address registered yet, or if we do but this route is shorter, save it and broadcast.
                            mRouter.setRoute(newAddress, hops, fromThread.getRemoteAddress());
                            mActivity.debugPrint("Saved ID route");

                            if(message.getDestination().equals(ALL)){
                                forwardBroadcastIDMsg(message, fromThread.getRemoteAddress());
                            }
                        }
                        break;

                    case IDS:

                        //TODO

                        break;
                }
                break;
        }
    }

    public int getCBSCountForMsg(String uuid){
        return cbsMsgTracker.get(uuid).size();
    }

    public Set<String> getCBSBannedListForMsg(String uuid){
        return cbsMsgTracker.get(uuid);
    }

    public void clearCBSTrack(String uuid){
        cbsMsgTracker.remove(uuid);
    }

    public void broadcastMsg(Message message){

        if(message.getDestination().equals(ALL)){

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(message);
            }

        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    /**
     * Forwards the message to any direct node that is not in the banned list.
     * The banned list is typically constituted by the peers who we already received the msg from.
     * @param message
     * @param bannedAddresses
     */
    public void forwardBroadcast(Message message, Set<String> bannedAddresses){

        if(message.getDestination().equals(ALL)){
            message.incrementHop();

            for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
                if(!bannedAddresses.contains(connectedEntry.getKey())){
                    connectedEntry.getValue().write(message);
                }
            }
        }else {
            mActivity.debugPrint("ERROR : This message is not meant for ALL");
        }
    }

    public void forwardBroadcastIDMsg(Message message, String fromAddress){

        if(message.getDestination().equals(ALL)){

            message.incrementHop();

            for(Map.Entry<String, ConnectedThread> connectedEntry : mRouter.getConnectedEntries()){
                if(!connectedEntry.getKey().equals(fromAddress)){
                    connectedEntry.getValue().write(message);
                }
            }
        }else{
            mActivity.debugPrint("ERROR : This message is not for ALL");
        }
    }

    public void forwardMsg(Message message){

        String destination = message.getDestination();
        if(mRouter.knows(destination)){
            message.incrementHop();
            mRouter.getRoutingThreadFor(destination).write(message);

        }else{
            mActivity.debugPrint("ERROR : forward destination unknown");
        }
    }

    public void sendMsg(String msg, String destination){

        if(destination.equals(ALL)){

            Message message = new Message(Message.Algo.CBS, Message.ContentType.MSG, msg, localBluetoothAddress, destination, 0);

            for(ConnectedThread connectedThread : mRouter.getConnectedThreads()){
                connectedThread.write(message);
            }
        }else if(mRouter.knows(destination)){

            Message message = new Message(Message.Algo.SINGLE, Message.ContentType.MSG, msg, localBluetoothAddress, destination, 0);
            mRouter.getRoutingThreadFor(destination).write(message);
            mActivity.debugPrint("ME : " + msg);

        }else if(destination.equals(localBluetoothAddress)){

            mActivity.debugPrint("ERROR : You cannot message yourself");
        }else {

            mActivity.debugPrint("ERROR : destination unknown");
        }
    }

    public void introduceMyselfToThread(ConnectedThread connectedThread){
        mActivity.debugPrint("Sending my ID");
        Message IDMessage = Message.getIDMsg(localBluetoothAddress);
        connectedThread.write(IDMessage);
    }

    public void sendMeshInfoTo(String newAddress){
        String knownPeersString = "";

        for(Map.Entry<String, Pair<Integer, String>> knownPeer : mRouter.getKnownPeers()){
            String knownAddress = knownPeer.getKey();

            if(!knownAddress.equals(newAddress)){
                knownPeersString += "<"+newAddress;
            }
        }

        //TODO
        Message knwonPeersMessage = new Message(Message.Algo.IDProp, Message.ContentType.IDS, knownPeersString, localBluetoothAddress, newAddress, 0);

        for(Map.Entry<String, Pair<Integer, String>> knownPeer : mRouter.getKnownPeers()){
            String knwonAddress = knownPeer.getKey();
            if(!knwonAddress.equals(newAddress)){
                mActivity.debugPrint("Sending peer : " + knwonAddress);
                int hops = knownPeer.getValue().first+1;
                Message message = new Message(Message.Algo.IDProp, Message.ContentType.ID, knwonAddress, localBluetoothAddress, newAddress, hops);
                mRouter.getRoutingThreadFor(newAddress).write(message);
            }
        }


    }

    private void printMsg(Message message){
        mActivity.debugPrint(message.getSource() + " : " + message.getMsg() + " (" + message.getHops() + ")");
    }
}
