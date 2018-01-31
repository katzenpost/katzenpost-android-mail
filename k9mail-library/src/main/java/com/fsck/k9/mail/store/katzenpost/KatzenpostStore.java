package com.fsck.k9.mail.store.katzenpost;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.DummyFolder;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
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
    private static final String PKI_ADDRESS = "37.218.242.147:29485";
    private static final String PKI_PINNED_PUBLIC_KEY =
            "DFD5E1A26E9B3EF7B3DA0102002B93C66FC36B12D14C608C3FBFCA03BF3EBCDC";

    private static Client client;

    private final File cacheDir;
    private final KatzenpostServerSettings settings;

    public static KatzenpostServerSettings decodeUri(String uri) {
        return KatzenpostUriParser.INSTANCE.decode(uri);
    }

    public static String createUri(KatzenpostServerSettings server) {
        return "katzenpost:" + server.linkkey + ":" + server.username + "@" + server.provider;
    }

    public KatzenpostStore(StoreConfig storeConfig, Context context) throws MessagingException {
        super(storeConfig, null);

        settings = KatzenpostStore.decodeUri(storeConfig.getStoreUri());
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
                Config config = createConfig();
                client = Katzenpost.new_(config);
                client.waitToConnect();
            } catch (Exception e) {
                throw new MessagingException("Error setting up Katzenpost client", e);
            }
        }

        return client;
    }

    @NonNull
    private Config createConfig() throws Exception {
        Config config = prepareConfig();

        Key key = Katzenpost.stringToKey(settings.linkkey);

        config.setProvider(settings.provider);
        config.setUser(settings.username);
        config.setLinkKey(key);

        return config;
    }

    @NonNull
    private Config prepareConfig() {
        LogConfig logConfig = new LogConfig();
        logConfig.setLevel("DEBUG");
        logConfig.setEnabled(true);

        Config config = new Config();
        config.setPkiAddress(PKI_ADDRESS);
        config.setPkiKey(PKI_PINNED_PUBLIC_KEY);
        config.setDataDir(cacheDir.getAbsolutePath());
        config.setLog(logConfig);
        return config;
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
