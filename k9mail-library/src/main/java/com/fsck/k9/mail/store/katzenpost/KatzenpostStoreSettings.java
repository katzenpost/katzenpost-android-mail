package com.fsck.k9.mail.store.katzenpost;


import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.webdav.WebDavStore;


/**
 * This class is used to store the decoded contents of an WebDavStore URI.
 *
 * @see WebDavStore#decodeUri(String)
 */
public class KatzenpostStoreSettings extends ServerSettings {
    protected KatzenpostStoreSettings(String host, int port, ConnectionSecurity connectionSecurity,
                                  AuthType authenticationType, String username, String password) {
        super(Type.KATZENPOST, host, port, connectionSecurity, authenticationType, username,
                password, null);
    }
}
