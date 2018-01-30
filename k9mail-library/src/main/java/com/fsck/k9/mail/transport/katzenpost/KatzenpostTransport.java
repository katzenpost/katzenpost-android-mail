package com.fsck.k9.mail.transport.katzenpost;


import java.util.Collections;

import android.content.Context;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.store.katzenpost.KatzenpostStore;


public class KatzenpostTransport extends Transport {
    private final KatzenpostStore store;

    public KatzenpostTransport(StoreConfig storeConfig, Context context) throws MessagingException {
        store = new KatzenpostStore(storeConfig, context);
    }

    @Override
    public void open() throws MessagingException {
        // nvm
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        store.sendMessages(Collections.singletonList(message));
    }

    @Override
    public void close() {
        // nvm
    }
}
