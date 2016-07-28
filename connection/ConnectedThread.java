package com.react.gabriel.wbam.padoc.connection;

import com.react.gabriel.wbam.padoc.messaging.Message;

/**
 * Created by gabriel on 05/07/16.
 */
public interface ConnectedThread {

    void start();

    void setRemoteAddress(String remoteAddress);

    void setRemoteName(String remoteName);

    String getRemoteAddress();

    String getRemoteName();

    void write(Message message);

}
