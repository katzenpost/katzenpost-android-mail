
package com.fsck.k9.mail;


import android.support.annotation.WorkerThread;


public abstract class Transport {

    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;

    // RFC 1047
    protected static final int SOCKET_READ_TIMEOUT = 300000;

    @WorkerThread
    public abstract void open() throws MessagingException;

    @WorkerThread
    public abstract void sendMessage(Message message) throws MessagingException;

    @WorkerThread
    public abstract void close();
}
