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

import static org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType.HTTPS;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType.HTTPS_REMOTING;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.PASSWORD;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.USERNAME;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.getConnectorType;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.getServerURL;

/**
 * @author Jan Martiska
 */
public class InitialContextDirectoryWildFlyNamingClient extends InitialContextDirectory {


    public InitialContextDirectoryWildFlyNamingClient() throws NamingException {
        System.setProperty("org.jboss.ejb.client.view.annotation.scan.enabled", "true");  // without this, CompressionHint for requests won't work
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, getServerURL());
        env.put("jboss.naming.client.ejb.context", "true");
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "true");
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SSL_STARTTLS", "true");
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SSL_ENABLED", (getConnectorType().equals(HTTPS_REMOTING) ||
                getConnectorType().equals(HTTPS)));
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        if (TestEnvironment.getAuthenticationType() == ServerAuthenticationType.USER) {
            env.put(Context.SECURITY_PRINCIPAL, USERNAME);
            env.put(Context.SECURITY_CREDENTIALS, PASSWORD);
            env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS",
                    "JBOSS-LOCAL-USER");
        }
        ctx = new InitialContext(env);
    }

    @Override
    public <T> T lookup(String applicationName, String moduleName, Class<? extends T> beanClass, Class<T> beanInterface,
                        BeanType beanType, String distinctName) throws NamingException {
        if(distinctName != null && !distinctName.isEmpty())
            throw new UnsupportedOperationException("Distinct names are unsupported when using remote-naming");
        return getBean(createLookupName(applicationName, moduleName, beanClass, beanInterface), beanInterface);
    }

    public <T> String createLookupName(String applicationName, String moduleName, Class<? extends T> beanClass, Class<T> beanInterface) {
        return (applicationName != null ? applicationName +"/" : "") + moduleName + "/" + beanClass.getSimpleName() + "!" + beanInterface.getName();
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
