package com.fsck.k9.mail.store.katzenpost;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.DummyFolder;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;
import katzenpost.Client;
import katzenpost.Config;
import katzenpost.Katzenpost;
import katzenpost.Key;
import katzenpost.LogConfig;
import katzenpost.Message;


/**
 * <pre>
 * Uses WebDAV formatted HTTP calls to an MS Exchange server to fetch email
 * and email information.
 * </pre>
 */
@SuppressWarnings("deprecation")
public class KatzenpostStore extends RemoteStore {
    public static final String FOLDER_INBOX = "inbox";
    private final File cacheDir;

    private static Client client;

    public static KatzenpostStoreSettings decodeUri(String uri) {
        return new KatzenpostStoreSettings("37.218.242.147", 29485, null, null, "bob@ramix", null);
    }

    public static String createUri(ServerSettings server) {
        return "katzenpost:" + server.username + "@" + server.host;
    }

    private String username;

    public KatzenpostStore(StoreConfig storeConfig, Context context) throws MessagingException {
        super(storeConfig, null);

        KatzenpostStoreSettings settings;
        try {
            settings = KatzenpostStore.decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        username = settings.username;
        cacheDir = new File(context.getCacheDir(), "katzencache");
    }

    @Override
    public void checkSettings() throws MessagingException {

    }

    @Override
    public List<? extends Folder> getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        return Collections.emptyList();
    }

    @Override
    public Folder<?> getFolder(String name) {
        return new DummyFolder();
    }

    @Override
    public boolean isSendCapable() {
        return true;
    }

    @Override
    public void sendMessages(List<? extends com.fsck.k9.mail.Message> messages) throws MessagingException {
        for (com.fsck.k9.mail.Message message : messages) {
            sendMessage(message);
        }
    }

    private void sendMessage(com.fsck.k9.mail.Message message) throws MessagingException {
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

    private void sendMessageTo(Address address, com.fsck.k9.mail.Message message) throws MessagingException {
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

    private Client getClientAndConnectIfNecessary() throws MessagingException {
        if (client == null) {
            try {
                Key key = Katzenpost.stringToKey("97f906cc6acd1ab84d3e66cfa6c1526febaa5d0cc73342def908dd2197aad6f4");

                LogConfig logConfig = new LogConfig();
                logConfig.setLevel("DEBUG");
                logConfig.setEnabled(true);

                Config config = new Config();
                config.setPkiAddress("37.218.242.147:29485");
                config.setPkiKey("DFD5E1A26E9B3EF7B3DA0102002B93C66FC36B12D14C608C3FBFCA03BF3EBCDC");
                config.setUser("eve");
                config.setProvider("ramix");
                config.setLinkKey(key);
                config.setDataDir(cacheDir.getAbsolutePath());
                config.setLog(logConfig);

                client = Katzenpost.new_(config);
                client.waitToConnect();
            } catch (Exception e) {
                throw new MessagingException("Error setting up Katzenpost client", e);
            }
        }

        return client;
    }

    public String getMessage(long timeout) throws MessagingException {
        Client client = getClientAndConnectIfNecessary();

        try {
            Message message = client.getMessage(timeout);
            if (message == null) {
                return null;
            }

            return message.getPayload();
        } catch (Exception e) {
            throw new MessagingException("Error fetching Katzenpost message", e);
        }

    }
}
