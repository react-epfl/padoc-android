package com.react.gabriel.wbam.padoc;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by gabriel on 13/05/16.
 */
public class Message {

    //UUID
    private static final String ID = "id";

    //Type of algorithm used
    private static final String ALGO = "algo";
    private static final String ALGO_FLOOD = "algo-flood";
    private static final String ALGO_CBS = "algo-cbs";
    private static final String ALGO_ROUTE = "algo-route";

    //Type of content
    private static final String CONTENT = "content";
    private static final String CONTENT_ID = "content-id";
    private static final String CONTENT_ID_OFFLINE = "content-id-offline";
    private static final String CONTENT_IDS = "content-ids";
    private static final String CONTENT_MSG = "content-msg";
    private static final String CONTENT_PRIORITY = "content-priority";
    private static final String CONTENT_ACK_REQUEST = "content-ack-request";
    private static final String CONTENT_ACK = "content-ack";

    //Content fields
    public static final String ADDRESS = "addr";
    public static final String NAME = "name";
    public static final String MESH = "mesh";

    //Source and destination fields
    private static final String SOURCE = "source";
    private static final String DESTINATION = "destination";
    public static final String ALL = "ALL";

    //Number of hops. This is the only field that changes with every forward.
    private static final String HOPS = "hops";


    //TODO : maybe every JSON field should be declared as an Objet field, some finals, like the ID.

    public enum Type {
        ACK_REQUEST, ACK, ID, ID_OFFLINE, IDS, MSG, PRIORITY;
    }

    public enum Algo {
        FLOOD, CBS, ROUTE;
    }

    private JSONObject jsonMsg = null;

    public Message(Algo algo, Type type, String content, String source, String destination, int hops){

        this.jsonMsg = new JSONObject();

        try {
            jsonMsg.put(ID, UUID.randomUUID());

            //Algo
            switch (algo){
                case ROUTE:
                    jsonMsg.put(ALGO, ALGO_ROUTE);
                    break;
                case CBS:
                    jsonMsg.put(ALGO, ALGO_CBS);
                    break;
                case FLOOD:
                    jsonMsg.put(ALGO, ALGO_FLOOD);
                    break;
            }

            //Type
            switch (type){
                case ID:
                    jsonMsg.put(CONTENT, CONTENT_ID);
                    break;
                case ID_OFFLINE:
                    jsonMsg.put(CONTENT, CONTENT_ID_OFFLINE);
                    break;
                case IDS:
                    jsonMsg.put(CONTENT, CONTENT_IDS);
                    break;
                case MSG:
                    jsonMsg.put(CONTENT, CONTENT_MSG);
                    break;
                case PRIORITY:
                    jsonMsg.put(CONTENT, CONTENT_PRIORITY);
                    break;
                case ACK_REQUEST:
                    jsonMsg.put(CONTENT, CONTENT_ACK_REQUEST);
                    break;
                case ACK:
                    jsonMsg.put(CONTENT, CONTENT_ACK);
            }

            jsonMsg.put(CONTENT_MSG, content);
            jsonMsg.put(SOURCE, source);
            jsonMsg.put(DESTINATION, destination);
            jsonMsg.put(HOPS, hops);

        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public Message(){};

    public boolean setMessage(String jsonString){
        //TODO : need to verify content
        boolean validJSONString = false;
        try{
            this.jsonMsg = new JSONObject(jsonString);
            validJSONString = true;
        }catch (JSONException e){
            e.printStackTrace();
            System.out.println("Error from jsonString: ");
            System.out.println(jsonString);
        }

        return validJSONString;
    }

    public static Message getIDMessage(String localAddress, String name, String mesh){

        JSONObject jsonMessageContent = new JSONObject();

        try {

            jsonMessageContent.put(ADDRESS, localAddress);
            jsonMessageContent.put(NAME, name);
            jsonMessageContent.put(MESH, mesh);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new Message(Algo.FLOOD, Type.ID, jsonMessageContent.toString(), localAddress, ALL, 0);
    }

    public static Message getIDOfflineMessage(String localAddress, String offlineAddress){

        return new Message(Algo.FLOOD, Type.ID_OFFLINE, offlineAddress, localAddress, ALL, 0);
    }

    public static Message getACKRequestMessage(String localAddress, String destination){

        return new Message(Algo.ROUTE, Type.ACK_REQUEST, null, localAddress, destination, 0);
    }

    public static Message getACKResponseMessage(String localAddres, String destination){

        return new Message(Algo.ROUTE, Type.ACK, null, localAddres, destination, 0);
    }

    public Type getType(){

        String content = null;

        try{
            content = jsonMsg.getString(CONTENT);
        }catch (JSONException e){
            e.printStackTrace();
        }

        switch (content){
            case CONTENT_ID:
                return Type.ID;
            case CONTENT_ID_OFFLINE:
                return Type.ID_OFFLINE;
            case CONTENT_IDS:
                return Type.IDS;
            case CONTENT_MSG:
                return Type.MSG;
            case CONTENT_ACK_REQUEST:
                return Type.ACK_REQUEST;
            case CONTENT_ACK:
                return Type.ACK;
            default: return null;
        }
    }

    public Algo getAlgo(){

        String algo = null;

        try{
            algo = jsonMsg.getString(ALGO);
        }catch(JSONException e){
            e.printStackTrace();
        }

        switch(algo){
            case ALGO_CBS:
                return Algo.CBS;
            case ALGO_ROUTE:
                return Algo.ROUTE;
            case ALGO_FLOOD:
                return  Algo.FLOOD;
            default: return null;
        }
    }

    public String getSource(){
        String source = null;

        try{
            source = this.jsonMsg.getString(SOURCE);
        }catch (JSONException e){
            e.printStackTrace();
        }

        return source;
    }

    public String getDestination(){
        String destination = null;

        try{
            destination = this.jsonMsg.getString(DESTINATION);
        }catch (JSONException e){
            e.printStackTrace();
        }

        return destination;
    }

    public String getMsg(){
        String msg = null;

        try{
            msg = this.jsonMsg.getString(CONTENT_MSG);
        }catch (JSONException e){
            e.printStackTrace();
        }

        return msg;
    }

    public int getHops(){
        Integer hops = null;

        try{
            hops = this.jsonMsg.getInt(HOPS);
        }catch (JSONException e){
            e.printStackTrace();
        }

        return hops;
    }

    public void incrementHop(){
        try {

            int currentHops = this.jsonMsg.getInt(HOPS);
            this.jsonMsg.put(HOPS, currentHops+1);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        return jsonMsg.toString();
    }

    public String getUUID(){
        String msgID = null;

        try{
            msgID = jsonMsg.getString(ID);
        }catch (JSONException e){
            e.printStackTrace();
        }

        return msgID;
    }
}