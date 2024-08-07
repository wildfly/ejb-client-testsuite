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

package org.wildfly.ejbclient.testsuite.integration.multinode.remotingoptions;

import java.nio.channels.ClosedChannelException;
import java.util.Properties;
import jakarta.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers.isIPv6;

/**
 * see REM3-295
 */

@RunWith(Arquillian.class)
@RunAsClient
public class RemotingOptionsTestCase {

    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "long-running-bean.jar");
        jar.addClasses(LongRunningBean.class, LongRunningBeanStateless.class);
        return jar;
    }

    private final JavaArchive DEPLOYMENT = createDeployment();

    @ArquillianResource
    private ContainerController containerController;

    OnlineManagementClient creaper;

    @Before
    public void before() throws Exception {
        setLoggerPrefix("NODE1", NODE1.homeDirectory, NODE1.configurationXmlFile);
        containerController.start(NODE1.nodeName);
        creaper = createCreaper(NODE1.bindAddress, NODE1.managementPort);
        DeploymentHelpers.deploy(DEPLOYMENT, creaper);
    }

    @After
    public void cleanup() throws Exception {
        DeploymentHelpers.undeploy(DEPLOYMENT.getName(), creaper);
        containerController.stop(NODE1.nodeName);
        setLoggerPrefix("", NODE1.homeDirectory, NODE1.configurationXmlFile);
    }

    /**
     * The invocation should fail because:
     * - the invocation itself takes ~5000 ms
     * - the heartbeat interval is 5000 ms
     * - the read timeout is 1500 ms
     * <p>
     * So the read timeout will be reached between two heartbeats, failing the invocation.
     */
    @Test
    public void readTimeout() throws NamingException {
        setWildflyConfigXml("read-timeout");

        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        final LongRunningBean bean = (LongRunningBean) ejbCtx
                .lookup("ejb:/long-running-bean/" + LongRunningBeanStateless.class.getSimpleName() + "!"
                        + LongRunningBean.class.getName());
        try {
            bean.doWork(5);
            Assert.fail("Invocation should fail with EJBException caused by ClosedChannelException");
        } catch (EJBException ex) {
            Assert.assertEquals("Invocation should fail with EJBException caused by ClosedChannelException",
                    ClosedChannelException.class, ex.getCause().getClass());
        }
    }


    public void setWildflyConfigXml(String name) {
        if ( !isIPv6() ) {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
                    "org/wildfly/ejbclient/testsuite/integration/multinode/remotingoptions/configs/" + name + ".xml").toString());
        } else {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
                    "org/wildfly/ejbclient/testsuite/integration/multinode/remotingoptions/configs/" + name + "-ipv6.xml").toString());
        }
    }

}
