package com.fsck.k9.mail.store.imap;


import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.TraditionalServerSettings;
import com.fsck.k9.mail.store.RemoteStore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class ImapStoreUriTest {
    @Test
    public void testDecodeStoreUriImapNoAuth() {
        String uri = "imap://user:pass@server/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapNoPassword() {
        String uri = "imap://user:@server/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapPlainNoPassword() {
        String uri = "imap://PLAIN:user:@server/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapExternalAuth() {
        String uri = "imap://EXTERNAL:user:clientCertAlias@server/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.EXTERNAL, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals("clientCertAlias", settings.clientCertificateAlias);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapXOAuth2() {
        String uri = "imap://XOAUTH2:user:@server/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.XOAUTH2, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals(null, settings.clientCertificateAlias);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapSSL() {
        String uri = "imap+tls+://PLAIN:user:pass@server/";
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, settings.connectionSecurity);
        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapTLS() {
        String uri = "imap+ssl+://PLAIN:user:pass@server/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, settings.connectionSecurity);
        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapAllExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapNoExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
    }

    @Test
    public void testDecodeStoreUriImapPrefixOnly() {
        String uri = "imap://PLAIN:user:pass@server:143/customPathPrefix";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapEmptyPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7C";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapAutodetectAndPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/1%7CcustomPathPrefix";
        
        TraditionalServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
        assertNull(settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testCreateStoreUriImapPrefix() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "customPathPrefix");
        TraditionalServerSettings
                settings = new TraditionalServerSettings(TraditionalServerSettings.Type.IMAP, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix", uri);
    }

    @Test
    public void testCreateStoreUriImapEmptyPrefix() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "");
        TraditionalServerSettings
                settings = new TraditionalServerSettings(TraditionalServerSettings.Type.IMAP, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7C", uri);
    }

    @Test
    public void testCreateStoreUriImapNoExtra() {
        TraditionalServerSettings
                settings = new TraditionalServerSettings(TraditionalServerSettings.Type.IMAP, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C", uri);
    }

    @Test
    public void testCreateStoreUriImapAutoDetectNamespace() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "true");

        TraditionalServerSettings
                settings = new TraditionalServerSettings(TraditionalServerSettings.Type.IMAP, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C", uri);
    }

    @Test
    public void testCreateDecodeStoreUriWithSpecialCharactersInUsernameAndPassword() {
        TraditionalServerSettings
                settings = new TraditionalServerSettings(TraditionalServerSettings.Type.IMAP, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user@doma:n", "p@ssw:rd%", null, null);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user%2540doma%253An:p%2540ssw%253Ard%2525@server:143/1%7C", uri);

        TraditionalServerSettings outSettings = RemoteStore.decodeStoreUri(uri);

        assertEquals("user@doma:n", outSettings.username);
        assertEquals("p@ssw:rd%", outSettings.password);
    }
}
