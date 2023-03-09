package org.wildfly.ejbclient.testsuite.integration.basic.utils;

/**
 * @author Jan Martiska
 */
public enum ConnectorType {

    // the first string (name) identifies the connector type, the second string (scheme) is its remoting URI scheme
    HTTP_REMOTING("http-remoting", "http-remoting"),
    HTTPS_REMOTING("https-remoting", "https-remoting"),
    REMOTING("remoting", "remote"),
    REMOTING_SSL("remoting-ssl", "remote"),
    HTTP("http", "http"),
    HTTPS("https", "https");

    private final String str;
    private final String scheme;

    ConnectorType(String name, String scheme) {
        this.str = name;
        this.scheme = scheme;
    }

    @Override
    public String toString() {
        return str;
    }

    public static ConnectorType getValue(String requestedValue) {
        for (ConnectorType ct : ConnectorType.values()) {
            if (ct.toString().equalsIgnoreCase(requestedValue)) {
                return ct;
            }
        }
        throw new IllegalArgumentException("Unknown connector type: " + requestedValue);
    }

    public String getConnectionScheme() {
        return scheme;
    }

}
