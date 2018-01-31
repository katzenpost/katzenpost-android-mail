package com.fsck.k9.mail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an abstraction to get rid of the store- and transport-specific URIs.
 *
 * <p>
 * Right now it's only used for settings import/export. But the goal is to get rid of
 * store/transport URIs altogether.
 * </p>
 *
 * @see com.fsck.k9.mail.store.StoreConfig#getStoreUri()
 * @see com.fsck.k9.mail.store.StoreConfig#getTransportUri()
 */
public class TraditionalServerSettings extends ServerSettings {
    /**
     * The host name of the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String host;

    /**
     * The port number of the server.
     *
     * {@code -1} if not applicable for the store or transport.
     */
    public final int port;

    /**
     * The type of connection security to be used when connecting to the server.
     *
     * {@link ConnectionSecurity#NONE} if not applicable for the store or transport.
     */
    public final ConnectionSecurity connectionSecurity;

    /**
     * The authentication method to use when connecting to the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final AuthType authenticationType;

    /**
     * The password part of the credentials needed to authenticate to the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String password;

    /**
     * The alias to retrieve a client certificate using Android 4.0 KeyChain API
     * for TLS client certificate authentication with the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String clientCertificateAlias;

    /**
     * Store- or transport-specific settings as key/value pair.
     *
     * {@code null} if not applicable for the store or transport.
     */
    private final Map<String, String> extra;


    /**
     * Creates a new {@code ServerSettings} object.
     *
     * @param type
     *         see {@link TraditionalServerSettings#type}
     * @param host
     *         see {@link TraditionalServerSettings#host}
     * @param port
     *         see {@link TraditionalServerSettings#port}
     * @param connectionSecurity
     *         see {@link TraditionalServerSettings#connectionSecurity}
     * @param authenticationType
     *         see {@link TraditionalServerSettings#authenticationType}
     * @param username
     *         see {@link TraditionalServerSettings#username}
     * @param password
     *         see {@link TraditionalServerSettings#password}
     * @param clientCertificateAlias
     *         see {@link TraditionalServerSettings#clientCertificateAlias}
     */
    public TraditionalServerSettings(Type type, String host, int port,
            ConnectionSecurity connectionSecurity, AuthType authenticationType, String username,
            String password, String clientCertificateAlias) {
        super(type, username);
        this.host = host;
        this.port = port;
        this.connectionSecurity = connectionSecurity;
        this.authenticationType = authenticationType;
        this.password = password;
        this.clientCertificateAlias = clientCertificateAlias;
        this.extra = null;
    }

    /**
     * Creates a new {@code ServerSettings} object.
     *
     * @param type
     *         see {@link TraditionalServerSettings#type}
     * @param host
     *         see {@link TraditionalServerSettings#host}
     * @param port
     *         see {@link TraditionalServerSettings#port}
     * @param connectionSecurity
     *         see {@link TraditionalServerSettings#connectionSecurity}
     * @param authenticationType
     *         see {@link TraditionalServerSettings#authenticationType}
     * @param username
     *         see {@link TraditionalServerSettings#username}
     * @param password
     *         see {@link TraditionalServerSettings#password}
     * @param clientCertificateAlias
     *         see {@link TraditionalServerSettings#clientCertificateAlias}
     * @param extra
     *         see {@link TraditionalServerSettings#extra}
     */
    public TraditionalServerSettings(Type type, String host, int port,
            ConnectionSecurity connectionSecurity, AuthType authenticationType, String username,
            String password, String clientCertificateAlias, Map<String, String> extra) {
        super(type, username);
        this.host = host;
        this.port = port;
        this.connectionSecurity = connectionSecurity;
        this.authenticationType = authenticationType;
        this.password = password;
        this.clientCertificateAlias = clientCertificateAlias;
        this.extra = (extra != null) ?
                Collections.unmodifiableMap(new HashMap<String, String>(extra)) : null;
    }

    /**
     * Creates an "empty" {@code ServerSettings} object.
     *
     * Everything but {@link TraditionalServerSettings#type} is unused.
     *
     * @param type
     *         see {@link TraditionalServerSettings#type}
     */
    public TraditionalServerSettings(Type type) {
        super(type, null);
        host = null;
        port = -1;
        connectionSecurity = ConnectionSecurity.NONE;
        authenticationType = null;
        password = null;
        clientCertificateAlias = null;
        extra = null;
    }

    /**
     * Returns store- or transport-specific settings as key/value pair.
     *
     * @return additional set of settings as key/value pair.
     */
    public Map<String, String> getExtra() {
        return extra;
    }

    protected void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public TraditionalServerSettings newPassword(String newPassword) {
        return new TraditionalServerSettings(type, host, port, connectionSecurity, authenticationType,
                username, newPassword, clientCertificateAlias);
    }

    public ServerSettings newClientCertificateAlias(String newAlias) {
        return new TraditionalServerSettings(type, host, port, connectionSecurity, AuthType.EXTERNAL,
                username, password, newAlias);
    }

    @Override
    public boolean isUnconfiguredOutgoing() {
        return AuthType.EXTERNAL != authenticationType
                && !(TraditionalServerSettings.Type.WebDAV == type)
                && username != null
                && !username.isEmpty()
                && (password == null || password
                .isEmpty());
    }

    @Override
    public boolean isUnconfiguredIncoming() {
        return AuthType.EXTERNAL != authenticationType && (password == null || password.isEmpty());
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TraditionalServerSettings)) {
            return false;
        }
        TraditionalServerSettings that = (TraditionalServerSettings) obj;
        return type == that.type &&
                port == that.port &&
                connectionSecurity == that.connectionSecurity &&
                authenticationType == that.authenticationType &&
                (host == null ? that.host == null : host.equals(that.host)) &&
                (username == null ? that.username == null : username.equals(that.username)) &&
                (password == null ? that.password == null : password.equals(that.password)) &&
                (clientCertificateAlias == null ? that.clientCertificateAlias == null :
                        clientCertificateAlias.equals(that.clientCertificateAlias));
    }
}