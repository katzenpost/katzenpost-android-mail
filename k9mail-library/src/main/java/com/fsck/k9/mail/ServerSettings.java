package com.fsck.k9.mail;


public abstract class ServerSettings {
    /**
     * Name of the store or transport type (e.g. IMAP).
     */
    public final Type type;
    /**
     * The username part of the credentials needed to authenticate to the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String username;

    protected ServerSettings(Type type, String username) {
        this.type = type;
        this.username = username;
    }

    public boolean isUnconfiguredOutgoing() {
        return false;
    }

    public boolean isUnconfiguredIncoming() {
        return false;
    }

    public enum Type {
        IMAP(143, 993),
        SMTP(587, 465),
        WebDAV(80, 443),
        POP3(110, 995),
        KATZENPOST(0, 0);

        public final int defaultPort;

        /**
         * Note: port for connections using TLS (=SSL) immediately
         * from the initial TCP connection.
         *
         * STARTTLS uses the defaultPort, then upgrades.
         *
         * See https://www.fastmail.com/help/technical/ssltlsstarttls.html.
         */
        public final int defaultTlsPort;

        private Type(int defaultPort, int defaultTlsPort) {
            this.defaultPort = defaultPort;
            this.defaultTlsPort = defaultTlsPort;
        }
    }
}
