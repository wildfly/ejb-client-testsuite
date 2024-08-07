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

package org.wildfly.ejbclient.testsuite.integration.multinode.failover;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.EJBClient;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ContainerHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.failover.beans.CounterBean;
import org.wildfly.ejbclient.testsuite.integration.multinode.failover.beans.CounterBeanStateful;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

/**
 * @author Michal Vinkler
 */
@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class FailoverViaWildflyConfigXmlStatefulBeanTestCase {

    private static final JavaArchive deployment = createDeployment();
    private static OnlineManagementClient creaper_node1;
    private static OnlineManagementClient creaper_node2;

    private static Logger logger = Logger.getLogger(FailoverViaWildflyConfigXmlStatefulBeanTestCase.class.getName());

    @ArquillianResource
    private ContainerController containerController;

    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "basic-failover-stateful.jar");
        jar.addClasses(CounterBeanStateful.class, CounterBean.class);
        return jar;
    }

    @BeforeClass
    public static void prepare() throws IOException {
        if (MiscHelpers.isIPv6()) {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
                    "org/wildfly/ejbclient/testsuite/integration/multinode/failover/wildfly-config-failover-ipv6.xml").toString());
        } else {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
                    "org/wildfly/ejbclient/testsuite/integration/multinode/failover/wildfly-config-failover.xml").toString());
        }
        creaper_node1 = ManagementHelpers
                .createCreaper(CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.managementPort);
        creaper_node2 = ManagementHelpers
                .createCreaper(CLUSTER1_NODE2.bindAddress, CLUSTER1_NODE2.managementPort);
    }

    @AfterClass
    public static void afterClass() {
        ManagementHelpers.safeClose(creaper_node1, creaper_node2);
    }

    @Before
    public void start() throws Exception {
        setLoggerPrefix("CLUSTER1_NODE1", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
        setLoggerPrefix("CLUSTER1_NODE2", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
        containerController.start(CLUSTER1_NODE1.nodeName);
        containerController.start(CLUSTER1_NODE2.nodeName);

        DeploymentHelpers.deploy(deployment, creaper_node1);
        DeploymentHelpers.deploy(deployment, creaper_node2);
    }

    /**
     * - Run two cluster nodes
     * - Obtain a stateful EJB proxy (cluster affinity is set automatically)
     * - and then repeat X times:
     * -- Invoke the EJB
     * -- Undeploy the app on node1
     * -- Invoke the EJB, assert that it was handled by node2 and the state was preserved
     * -- Deploy the app on node1 again
     * -- Undeploy the app on node2
     * -- Invoke the EJB, assert that it was handled by node1 and the state was preserved
     * -- Deploy the app on node2 again
     */
    @Test
    public void testSimpleFailover_undeploy() throws NamingException, CommandFailedException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            String lookup = "ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                    + CounterBean.class.getName() + "?stateful";
            final CounterBean bean = (CounterBean) ejbCtx
                    .lookup(lookup);


            for (int iteration = 0; iteration < 10; iteration++) {
                System.out.println("***** starting iteration " + iteration);
                for (int i = 0; i < 10; i++) {
                    String node = bean.getNode();
                    Assert.assertTrue("Unexpected node handled the invocation: " + node,
                            Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(node));
                }
                int counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3 * iteration + 1, counter);
                DeploymentHelpers.undeploy(deployment.getName(), creaper_node1);
                for (int i = 0; i < 10; i++) {
                    Assert.assertEquals("(invocation " + i + ") should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
                }
                counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3 * iteration + 2, counter);
                DeploymentHelpers.deploy(deployment, creaper_node1);
                DeploymentHelpers.undeploy(deployment.getName(), creaper_node2);
                for (int i = 0; i < 10; i++) {
                    Assert.assertEquals("(invocation " + i + ") should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
                }
                counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3 * iteration + 3, counter);
                DeploymentHelpers.deploy(deployment, creaper_node2);
            }
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx);
        }
    }

    /**
     * - Run two cluster nodes
     * - Obtain a stateful EJB proxy (cluster affinity is set automatically)
     * - and then repeat X times:
     * -- Invoke the EJB
     * -- Stop node1
     * -- Invoke the EJB, assert that it was handled by node2 and the state was preserved
     * -- Start node1 again
     * -- Stop node2
     * -- Invoke the EJB, assert that it was handled by node1 and the state was preserved
     * -- Start node2
     */
    @Test
    public void testSimpleFailover_shutdown() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            final CounterBean bean = (CounterBean) ejbCtx
                    .lookup("ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                            + CounterBean.class.getName() + "?stateful");

            for (int iteration = 0; iteration < 10; iteration++) {
                System.out.println("***** starting iteration " + iteration);
                String node = bean.getNode();
                Assert.assertTrue("Unexpected node handled the invocation: " + node,
                        Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(node));
                int counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3 * iteration + 1, counter);
                containerController.stop(CLUSTER1_NODE1.nodeName);
                for (int i = 0; i < 10; i++) {
                    Assert.assertEquals("(invocation " + i + ") should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
                }
                counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3 * iteration + 2, counter);
                containerController.start(CLUSTER1_NODE1.nodeName);
                containerController.stop(CLUSTER1_NODE2.nodeName);
                for (int i = 0; i < 10; i++) {
                    Assert.assertEquals("(invocation " + i + ") should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
                }
                counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3 * iteration + 3, counter);
                containerController.start(CLUSTER1_NODE2.nodeName);
            }
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx);
        }
    }

    /**
     * - Run only one cluster node (node1)
     * - Obtain a stateful EJB proxy (cluster affinity is set automatically)
     * - Invoke the EJB few times
     * - Start node2
     * - Stop node1
     * -- Invoke the EJB, assert that it was handled by node2 and the state was preserved
     */
    @Test
    public void testSimpleFailover_shutdown_clusterScaleUp() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            System.out.println("***** Stopping all the nodes");
            containerController.stop(CLUSTER1_NODE1.nodeName);
            containerController.stop(CLUSTER1_NODE2.nodeName);
            System.out.println("***** Starting node1");
            containerController.start(CLUSTER1_NODE1.nodeName);

            final CounterBean bean = (CounterBean) ejbCtx
                    .lookup("ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                            + CounterBean.class.getName() + "?stateful");

            int counter;

            for (int i = 0; i < 3; i++) {
                Assert.assertEquals("(invocation should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
                counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", i + 1, counter);
            }

            System.out.println("***** Starting node2");
            containerController.start(CLUSTER1_NODE2.nodeName);
            System.out.println("***** Stopping node1");
            containerController.stop(CLUSTER1_NODE1.nodeName);
            for (int i = 0; i < 3; i++) {
                Assert.assertEquals("(invocation " + i + ") should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
                counter = bean.getCounterAndIncrement();
                Assert.assertEquals("Unexpected counter value returned by stateful bean: ", i + 4, counter);
            }

        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx);
        }
    }

    /**
     * Sets CLUSTER_AFFINITY manually via JNDI properties.
     * Repeat X times:
     * - create an EJB client context with cluster affinity while both nodes are up
     * - stop one randomly picked node
     * - create stateful EJB proxy and make sure it can be called
     * - start the stopped node again
     */
    @Test
    public void testSessionCreationWhenOneNodeIsDown() throws NamingException {
        for(int iteration = 0; iteration < 10; iteration++) {
            System.out.println("***** starting iteration " + iteration);
            final Properties properties = new Properties();
            properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
            properties.put(EJBClient.CLUSTER_AFFINITY, "ejb");
            final InitialContext ejbCtx = new InitialContext(properties);
            try {
                // stop one randomly picked node
                Containers.Container containerToStop = Math.random() > 0.5 ? CLUSTER1_NODE1 : CLUSTER1_NODE2;
                logger.info("Going to shut down " + containerToStop.nodeName + " now...");
                containerController.stop(containerToStop.nodeName);

                logger.info("Creating stateful EJB proxy...");
                final CounterBean bean = (CounterBean) ejbCtx
                        .lookup("ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                                + CounterBean.class.getName() + "?stateful");
                bean.hello();
                logger.info("Going to bring " + containerToStop.nodeName + " back up now...");
                containerController.start(containerToStop.nodeName);
            } finally {
                MiscHelpers.safeCloseEjbClientContext(ejbCtx);
            }

        }

    }

    /**
     * Sets CLUSTER_AFFINITY manually via JNDI properties. Sets Invocation Timeout programmatically.
     * - Run two cluster nodes
     * - Obtain a stateful EJB proxy
     * - Invoke the EJB
     * - Stop node1
     * - Invoke the EJB, assert that it was handled by node2 and the state was preserved
     * - Start node1 again
     * - Stop node2
     * - Invoke the EJB, assert that it was handled by node1 and the state was preserved
     */
    @Test
    public void testSimpleFailover_shutdown_invocationTimeout() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        properties.put(EJBClient.CLUSTER_AFFINITY, "ejb");
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            final CounterBean bean = (CounterBean) ejbCtx
                    .lookup("ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                            + CounterBean.class.getName() + "?stateful");

            EJBClient.setInvocationTimeout(bean, 10, TimeUnit.SECONDS); //causes EJBCLIENT-250

            String node = bean.getNode();
            Assert.assertTrue("Unexpected node handled the invocation: " + node,
                    Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(node));
            int counter = bean.getCounterAndIncrement();
            Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 1, counter);
            containerController.stop(CLUSTER1_NODE1.nodeName);
            Assert.assertEquals("should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
            counter = bean.getCounterAndIncrement();
            Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 2, counter);
            containerController.start(CLUSTER1_NODE1.nodeName);
            containerController.stop(CLUSTER1_NODE2.nodeName);
            Assert.assertEquals("should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
            counter = bean.getCounterAndIncrement();
            Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3, counter);
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx);
        }
    }


    @After
    public void cleanup() throws Exception {
        containerController.start(CLUSTER1_NODE1.nodeName);
        containerController.start(CLUSTER1_NODE2.nodeName);
        DeploymentHelpers.undeploy(deployment.getName(), creaper_node1);
        DeploymentHelpers.undeploy(deployment.getName(), creaper_node2);
        ContainerHelpers
                .stopContainers(containerController, CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName);
        setLoggerPrefix("", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
        setLoggerPrefix("", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
    }

}
