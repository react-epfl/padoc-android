package com.react.gabriel.wbam.padoc;

import com.google.gson.JsonObject;

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
    private static final String ALGO_SINGLE = "algo-route";

    //Type of content
    private static final String CONTENT = "content";
    private static final String CONTENT_ID = "content-id";
    private static final String CONTENT_IDS = "content-ids";
    private static final String CONTENT_MSG = "content-msg";
    private static final String CONTENT_PRIORITY = "content-priority";

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

    public enum ContentType {
        ID, IDS, MSG, PRIORITY;
    }

    public enum Algo {
        FLOOD, CBS, ROUTE;
    }

    private JSONObject jsonMsg = null;

    public Message(Algo algo, ContentType contentType, String content, String source, String destination, int hops){

        this.jsonMsg = new JSONObject();

        try {
            jsonMsg.put(ID, UUID.randomUUID());

            //Algo
            if(algo.equals(Algo.ROUTE)){
                jsonMsg.put(ALGO, ALGO_SINGLE);
            }else if(algo.equals(Algo.CBS)){
                jsonMsg.put(ALGO, ALGO_CBS);
            }else if(algo.equals(Algo.FLOOD)){
                jsonMsg.put(ALGO, ALGO_FLOOD);
            }

            //ContentType
            if(contentType.equals(ContentType.ID)){
                jsonMsg.put(CONTENT, CONTENT_ID);
            } else if (contentType.equals(ContentType.IDS)) {
                jsonMsg.put(CONTENT, CONTENT_IDS);
            } else if (contentType.equals(ContentType.MSG)) {
                jsonMsg.put(CONTENT, CONTENT_MSG);
            } else if (contentType.equals(ContentType.PRIORITY)) {
                jsonMsg.put(CONTENT, CONTENT_PRIORITY);
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

    public static Message getIDMsg(String localAddress, String name, String mesh){

//        String msgContent = localAddress+"-"+name;

        JSONObject jsonMessageContent = new JSONObject();

        try {

            jsonMessageContent.put(ADDRESS, localAddress);
            jsonMessageContent.put(NAME, name);
            jsonMessageContent.put(MESH, mesh);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new Message(Algo.FLOOD, ContentType.ID, jsonMessageContent.toString(), localAddress, ALL, 0);
    }

    public ContentType getContentType(){

        String content = null;

        try{
            content = jsonMsg.getString(CONTENT);
        }catch (JSONException e){
            e.printStackTrace();
        }

        switch (content){
            case CONTENT_ID:
                return ContentType.ID;
            case CONTENT_IDS:
                return ContentType.IDS;
            case CONTENT_MSG:
                return ContentType.MSG;
            default:return null;
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
            case ALGO_SINGLE:
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