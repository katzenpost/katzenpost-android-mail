package com.fsck.k9.mail.transport.katzenpost;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.StoreConfig;
import katzenpost.Client;
import katzenpost.Config;
import katzenpost.Katzenpost;


public class KatzenpostTransport extends Transport {
    private final StoreConfig storeConfig;
    private Client client;

    public KatzenpostTransport(StoreConfig storeConfig) {
        this.storeConfig = storeConfig;
    }

    @Override
    public void open() throws MessagingException {
        Config config = new Config();
        try {
            client = Katzenpost.new_(config);
        } catch (Exception e) {
            throw new MessagingException("Error setting up Katzenpost client", e);
        }
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        open();

        List<Address> addresses = new ArrayList<>();
        {
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.TO)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.CC)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.BCC)));
        }
        message.setRecipients(RecipientType.BCC, null);

        for (Address address : addresses) {
            sendMessageTo(address, message);
        }
    }

    private void sendMessageTo(Address address, Message message) throws MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            message.writeTo(baos);
        } catch (IOException e) {
            throw new MessagingException("Error encoding message for transport!", e);
        }
        byte[] msgBytes = baos.toByteArray();

        try {
            client.send(address.getAddress(), new String(msgBytes));
        } catch (Exception e) {
            throw new MessagingException("Error encoding message for transport!", e);
        }
    }

    @Override
    public void close() {
        // client.shutdown();
    }
}
