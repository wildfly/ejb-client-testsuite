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

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.Version;




/**
 * @author Jan Martiska
 */
public class TestEnvironment {

    public static final String USERNAME = "joe";

    public static final String PASSWORD = "weAreAwesome2015!";

    public static final ConnectorType connectorType;

    public static final ServerAuthenticationType authenticationType;

    public static final EJBClientContextType contextType;

    public static final String NODE_NAME = "node0";

    public static final Integer JAVA_VERSION;

    static {
        connectorType = ConnectorType.getValue(System.getProperty("connector"));
        authenticationType = ServerAuthenticationType
                .valueOf(System.getProperty("authentication.type").toUpperCase());
        contextType = EJBClientContextType.getValue(System.getProperty("context.type"));

        // find java version
        String spec = System.getProperty("java.specification.version");
        JAVA_VERSION = "1.8".equals(spec) ? 8 : Integer.parseInt(spec);
    }

    public static ConnectorType getConnectorType() {
        return connectorType;
    }

    public static ServerAuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public static String getServerVersion() throws IOException {
        try (ModelControllerClient mcc = ModelControllerClient.Factory
                .create("127.0.0.1", 9990)) {
            ModelNode op = new ModelNode();
            op.get(ClientConstants.OP).set("product-info");
            final ModelNode result = mcc.execute(op);
            return result.get("result").get(0).get("summary").get("product-version").asString();
        }
    }

    public static int getServerPort() {
        switch (connectorType) {
            case HTTP_REMOTING:
                return 8080;
            case HTTPS_REMOTING:
                return 8443;
            case REMOTING:
                return 4447;
            case REMOTING_SSL:
                return 4448;
            case REMOTE_TLS:
                return 4449;
            case HTTP:
                return 8080;
            case HTTPS:
                return 8443;
            default:
                throw new Error("Meh.");
        }
    }

    public static String getServerURL() {
        String ret =
                connectorType.getConnectionScheme()
                        + "://127.0.0.1:"
                        + TestEnvironment.getServerPort();
        if (connectorType == ConnectorType.HTTP || connectorType == ConnectorType.HTTPS) {
            ret = ret + "/test-services";
        }
        return ret;
    }

    public static EJBClientContextType getContextType() {
        return contextType;
    }

    public static String getRawEJBClientProjectVersion() {
        return Version.getVersionString();
    }
}
