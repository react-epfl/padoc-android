package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.Padoc;
import com.react.gabriel.wbam.padoc.messaging.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by gabriel on 29/06/16.
 */
public class WifiConnectedThread extends Thread implements ConnectedThread {

    private Padoc padoc;

    private Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;

    private String remoteAddress;
    private String remoteName;

    public WifiConnectedThread(Padoc padoc, Socket socket){

        this.padoc = padoc;
        this.socket = socket;

        DataInputStream tmpInputStream = null;
        DataOutputStream tmpOutStream = null;

        try{
            tmpInputStream = new DataInputStream(socket.getInputStream());
            tmpOutStream = new DataOutputStream(socket.getOutputStream());
        }catch (IOException e){
            e.printStackTrace();
        }

        dataInputStream = tmpInputStream;
        dataOutputStream = tmpOutStream;

    }

    public void run(){

        if(dataInputStream != null && dataOutputStream != null){

            while (true){

                String messageFromClient = null;

                try {
                    messageFromClient = dataInputStream.readUTF();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(messageFromClient != null) padoc.handleRawJsonMessage(this, messageFromClient);
            }
        }
    }

    public void write(Message message){

        try {
            dataOutputStream.writeUTF(message.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setRemoteAddress(String remoteAddress){
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void setRemoteName(String remoteName){
        this.remoteName = remoteName;
    }

    @Override
    public String getRemoteAddress(){
        return this.remoteAddress;
    }

    @Override
    public String getRemoteName(){
        return this.remoteName;
    }

}
