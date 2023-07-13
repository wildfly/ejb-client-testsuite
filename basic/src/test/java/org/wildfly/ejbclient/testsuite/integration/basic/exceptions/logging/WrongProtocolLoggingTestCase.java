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

package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.logging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.PASSWORD;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.USERNAME;

/**
 * Tests improved logging when client is trying to connect with a different protocol/port on server and client.
 * Test tries different invalid protocol/port combinations and checks if the correct log message was present.
 * Test for [ EJBCLIENT-415 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WrongProtocolLoggingTestCase {
    private static final String DEPLOYMENT_NAME = "wrong-logging";

    private static final String LOOKUP = "ejb:/" + DEPLOYMENT_NAME + "//HelloBean!org.wildfly.ejbclient.testsuite.integration.basic.exceptions.logging.Hello";
    private static final Pattern PATTERN = Pattern.compile(".*Error in connecting to Destination.*: Please check if the client and server are configured to use the same protocol and ports.*");
    private static final String ASSERT_MESSAGE = "Logging message should alert user to check same protocol/port on the server and client side.";

    private static InitialContext ctx;

    @Deployment(name = DEPLOYMENT_NAME, testable = false)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME + ".jar");
        jar.addClasses(Hello.class, HelloBean.class);
        return jar;
    }

    @Test
    public void testRemoteHttp8443() throws NamingException {
        String protocol = ConnectorType.HTTP_REMOTING.getConnectionScheme();
        int port = 8443;

        callRemoteBeanShouldFail(protocol, port);
    }

    @Test
    public void testRemoteHttp4447() throws NamingException {
        String protocol = ConnectorType.HTTP_REMOTING.getConnectionScheme();
        int port = 4447;

        callRemoteBeanShouldFail(protocol, port);
    }

    @Test
    public void testRemoteHttps8080() throws NamingException {
        String protocol = ConnectorType.HTTPS_REMOTING.getConnectionScheme();
        int port = 8080;

        callRemoteBeanShouldFail(protocol, port);
    }

    @Test
    public void testRemoteHttps4447() throws NamingException {
        String protocol = ConnectorType.HTTPS_REMOTING.getConnectionScheme();
        int port = 4447;

        callRemoteBeanShouldFail(protocol, port);
    }

    @Test
    public void testRemote8080() throws NamingException {
        String protocol = ConnectorType.REMOTING.getConnectionScheme();
        int port = 8080;

        callRemoteBeanShouldFail(protocol, port);
    }

    private void callRemoteBeanShouldFail(String protocol, int port) throws NamingException {
        try {
            Hello ejb = getBean(protocol, port);
            try {
                ejb.sayHello();
                Assert.fail("Given combination should fail!");
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Assert.assertTrue(ASSERT_MESSAGE, PATTERN.matcher(sw.toString()).find());
            }
        } catch(Exception e1) {
            e1.printStackTrace();
            throw e1;
        }
    }

    @Test
    public void testCorrectCombination() throws Exception {
        String protocol = TestEnvironment.getConnectorType().getConnectionScheme();
        int port = TestEnvironment.getServerPort();

        try{
            Hello ejb = getBean(protocol, port);
            ejb.sayHello();
        } catch(Exception e1) {
            throw e1;
        }
    }

    private Hello getBean(String protocol, int port) throws NamingException {
        final Properties jndiProperties = new Properties();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        jndiProperties.put(Context.SECURITY_PRINCIPAL, USERNAME);
        jndiProperties.put(Context.SECURITY_CREDENTIALS, PASSWORD);

        String suffix = "";
        if (TestEnvironment.connectorType == ConnectorType.HTTP || TestEnvironment.connectorType == ConnectorType.HTTPS) {
            suffix = "/test-services";
        }
        jndiProperties.put(Context.PROVIDER_URL, String.format("%s://%s:%d%s", protocol, "127.0.0.1", port, suffix));

        ctx = new InitialContext(jndiProperties);
        return (Hello) ctx.lookup(LOOKUP);
    }

    @After
    public final void cleanUp() throws NamingException {
        ctx.close();
    }
}
