package com.fsck.k9.mail.store.katzenpost;


import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;
import katzenpost.Client;
import katzenpost.Config;
import katzenpost.Katzenpost;
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

    private Client client;

    public static KatzenpostStoreSettings decodeUri(String uri) {
        return new KatzenpostStoreSettings("37.218.242.147", 29485, null, null, "bob@ramix", null);
    }

    public static String createUri(ServerSettings server) {
        return "katzenpost:" + server.username + "@" + server.host;
    }

    private String username;

    public KatzenpostStore(StoreConfig storeConfig) throws MessagingException {
        super(storeConfig, null);

        KatzenpostStoreSettings settings;
        try {
            settings = KatzenpostStore.decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        username = settings.username;
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
        throw new UnsupportedOperationException();
    }

    private Client getClient() throws MessagingException {
        if (client == null) {
            try {
                Config config = new Config();
                client = Katzenpost.new_(config);
            } catch (Exception e) {
                throw new MessagingException("Error setting up Katzenpost client", e);
            }
        }
        return client;
    }

    public String getMessage(long timeout) throws MessagingException {
        try {
            Message message = getClient().getMessage(timeout);
            if (message == null) {
                return null;
            }

            return message.getPayload();
        } catch (Exception e) {
            throw new MessagingException("Error fetching Katzenpost message", e);
        }

    }
}
