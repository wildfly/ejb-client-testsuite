/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.junit.Assume;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeFalse("Legacy client doesn't log wrong protocol, therefor this test case should be skipped", TestEnvironment.isLegacyEjbClient());
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
