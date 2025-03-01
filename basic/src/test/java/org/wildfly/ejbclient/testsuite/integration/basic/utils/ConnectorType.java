/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    HTTPS("https", "https"),
    REMOTE_TLS("remote+tls", "remote+tls");

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
