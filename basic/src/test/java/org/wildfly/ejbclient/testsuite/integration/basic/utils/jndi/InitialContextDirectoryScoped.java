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

package org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.ejbclient.testsuite.integration.basic.utils.ServerAuthenticationType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;

import static org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType.HTTPS_REMOTING;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType.REMOTING_SSL;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.PASSWORD;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.USERNAME;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.getConnectorType;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.getServerPort;

/**
 * @author Jan Martiska
 */
public class InitialContextDirectoryScoped extends InitialContextDirectory {

    public InitialContextDirectoryScoped() throws NamingException {
        System.setProperty("org.jboss.ejb.client.view.annotation.scan.enabled", "true");  // without this, CompressionHint for requests won't work
        Properties env = new Properties();
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put("org.jboss.ejb.client.scoped.context", "true");
        env.put("endpoint.name", "client-endpoint");
        env.put("remote.connections", "main");
        env.put("remote.connection.main.host", "127.0.0.1");
        env.put("remote.connection.main.protocol", getConnectorType().getConnectionScheme());
        env.put("remote.connection.main.port", Integer.toString(getServerPort()));
        env.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        env.put("remote.connection.main.connect.options.org.xnio.Options.SSL_STARTTLS", "true");
        env.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "true");
        env.put("remote.connection.main.connect.options.org.xnio.Options.SSL_ENABLED",
                String.valueOf(getConnectorType().equals(HTTPS_REMOTING) || getConnectorType().equals(REMOTING_SSL)));
        if (TestEnvironment.getAuthenticationType() == ServerAuthenticationType.USER) {
            env.put("remote.connection.main.username", USERNAME);
            env.put("remote.connection.main.password", PASSWORD);
            env.put("remote.connection.main.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS",
                    "JBOSS-LOCAL-USER");
        }
        ctx = new InitialContext(env);
    }

    @Override
    public <T> T lookup(String applicationName, String moduleName, Class<? extends T> beanClass, Class<T> beanInterface,
                        BeanType beanType, String distinctName) throws NamingException {

        applicationName = applicationName == null ? "" : applicationName;
        String name = createJndiName(applicationName, moduleName, beanClass, beanInterface, beanType,
                distinctName);
        return getBean(name, beanInterface);
    }

    public <T> String createJndiName(String earName, String jarName, Class<?> beanClass,
                                     Class<T> beanInterface, BeanType type, String distinctName) {
        return String.format("ejb:%s/%s/%s/%s!%s%s",
                earName, jarName, distinctName == null ? "" : distinctName, beanClass.getSimpleName(),
                beanInterface.getName(),
                (type == BeanType.STATEFUL) ? "?stateful" : "");
    }

    @Override
    public void close() {
        try {
            ctx.close();
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
    }

}
