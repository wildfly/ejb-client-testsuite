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

package org.wildfly.ejbclient.testsuite.integration.multinode.twoclusters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Ignore;
import org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whereami.WhereAmI;
import org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whereami.WhereAmIStateful;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ContainerHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER2_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER2_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

/**
 * see WFLY-9419
 */

@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInvokingTwoClustersStatefulBeanTestCase {

    private static final JavaArchive deployment = createDeployment();
    public static final String DEPLOYMENT_NAME = "two-clusters";


    private String oldValueOfChannelAttribute;

    private static OnlineManagementClient creaper_cluster1node1;
    private static OnlineManagementClient creaper_cluster1node2;
    private static OnlineManagementClient creaper_cluster2node1;
    private static OnlineManagementClient creaper_cluster2node2;

    private static Logger logger = Logger.getLogger(ClientInvokingTwoClustersStatefulBeanTestCase.class.getName());


    @ArquillianResource
    private ContainerController containerController;

    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME + ".jar");
        jar.addClasses(WhereAmI.class, WhereAmIStateful.class);
        return jar;
    }

    @BeforeClass
    public static void prepare() throws IOException {
        creaper_cluster1node1 = ManagementHelpers
                .createCreaper(CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.managementPort);
        creaper_cluster1node2 = ManagementHelpers
                .createCreaper(CLUSTER1_NODE2.bindAddress, CLUSTER1_NODE2.managementPort);
        creaper_cluster2node1 = ManagementHelpers
                .createCreaper(CLUSTER2_NODE1.bindAddress, CLUSTER2_NODE1.managementPort);
        creaper_cluster2node2 = ManagementHelpers
                .createCreaper(CLUSTER2_NODE2.bindAddress, CLUSTER2_NODE2.managementPort);
    }

    @AfterClass
    public static void afterClass() {
        ManagementHelpers.safeClose(creaper_cluster1node1, creaper_cluster1node2, creaper_cluster2node1, creaper_cluster2node2);
    }

    @Before
    public void start() throws Exception {
        setLoggerPrefix("CLUSTER1_NODE1", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
        setLoggerPrefix("CLUSTER1_NODE2", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
        setLoggerPrefix("CLUSTER2_NODE1", CLUSTER2_NODE1.homeDirectory, CLUSTER2_NODE1.configurationXmlFile);
        setLoggerPrefix("CLUSTER2_NODE2", CLUSTER2_NODE2.homeDirectory, CLUSTER2_NODE2.configurationXmlFile);
        containerController.start(CLUSTER1_NODE1.nodeName);
        containerController.start(CLUSTER1_NODE2.nodeName);
        containerController.start(CLUSTER2_NODE1.nodeName);
        containerController.start(CLUSTER2_NODE2.nodeName);

        // I guess we can safely assume this is the same on all instances
        oldValueOfChannelAttribute = ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster1node1, "cluster1");        // set the channel on each instance
        logger.info("Old value of channel attribute: " + oldValueOfChannelAttribute);
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster1node2, "cluster1");
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster2node1, "cluster2");
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster2node2, "cluster2");
        new Administration(creaper_cluster1node1).reload();
        new Administration(creaper_cluster1node2).reload();
        new Administration(creaper_cluster2node1).reload();
        new Administration(creaper_cluster2node2).reload();

        DeploymentHelpers.deploy(deployment, creaper_cluster1node1);
        DeploymentHelpers.deploy(deployment, creaper_cluster1node2);
        DeploymentHelpers.deploy(deployment, creaper_cluster2node1);
        DeploymentHelpers.deploy(deployment, creaper_cluster2node2);
    }


    @Test
    @Ignore("https://issues.redhat.com/browse/WFLY-19909")
    public void testInvokingTwoClustersAlternately() throws Exception {
        final Properties propertiesCluster1 = new Properties();
        propertiesCluster1.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        propertiesCluster1.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector);
        final InitialContext ejbCtx_cluster1 = new InitialContext(propertiesCluster1);

        final Properties propertiesCluster2 = new Properties();
        propertiesCluster2.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        propertiesCluster2.put(Context.PROVIDER_URL, CLUSTER2_NODE1.urlOfHttpRemotingConnector);
        final InitialContext ejbCtx_cluster2 = new InitialContext(propertiesCluster2);


        try {
            for (int i1 = 0; i1 < 10; i1++) {
                final WhereAmI beanInCluster1 = (WhereAmI) ejbCtx_cluster1
                        .lookup("ejb:/" + DEPLOYMENT_NAME + "/" + WhereAmIStateful.class.getSimpleName() + "!"
                                + WhereAmI.class.getName() + "?stateful");
                final WhereAmI beanInCluster2 = (WhereAmI) ejbCtx_cluster2
                        .lookup("ejb:/" + DEPLOYMENT_NAME + "/" + WhereAmIStateful.class.getSimpleName() + "!"
                                + WhereAmI.class.getName() + "?stateful");
                for (int i2 = 0; i2 < 10; i2++) {
                    String nodeInCluster1 = beanInCluster1.getNode();
                    logger.info("Invocation handled by " + nodeInCluster1);
                    Assert.assertTrue("Invocations from this InitialContext should be routed to cluster1, but was routed to " + nodeInCluster1,
                            Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(nodeInCluster1));

                    String nodeInCluster2 = beanInCluster2.getNode();
                    logger.info("Invocation handled by " + nodeInCluster2);
                    Assert.assertTrue("Invocations from this InitialContext should be routed to cluster2, but was routed to " + nodeInCluster2,
                            Arrays.asList(CLUSTER2_NODE1.nodeName, CLUSTER2_NODE2.nodeName).contains(nodeInCluster2));
                }
            }
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx_cluster1);
            MiscHelpers.safeCloseEjbClientContext(ejbCtx_cluster2);
        }
    }

    @Test
    @Ignore("https://issues.redhat.com/browse/WFLY-19909")
    public void testInvokingTwoClustersOneAfterAnother() throws Exception {
        logger.info("WILL NOW START INVOKING ON CLUSTER1");
        final Properties propertiesCluster1 = new Properties();
        propertiesCluster1.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        propertiesCluster1.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector);
        final InitialContext ejbCtx_cluster1 = new InitialContext(propertiesCluster1);

        try {
            for (int i1 = 0; i1 < 10; i1++) {
                final WhereAmI beanInCluster1 = (WhereAmI) ejbCtx_cluster1
                        .lookup("ejb:/" + DEPLOYMENT_NAME + "/" + WhereAmIStateful.class.getSimpleName() + "!"
                                + WhereAmI.class.getName() + "?stateful");
                for (int i2 = 0; i2 < 5; i2++) {
                    String nodeInCluster1 = beanInCluster1.getNode();
                    logger.info("Invocation handled by " + nodeInCluster1);
                    Assert.assertTrue("Invocations from this InitialContext should be routed to cluster1, but was routed to " + nodeInCluster1,
                            Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(nodeInCluster1));
                }
            }
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx_cluster1);
        }

        logger.info("WILL NOW START INVOKING ON CLUSTER2");
        final Properties propertiesCluster2 = new Properties();
        propertiesCluster2.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        propertiesCluster2.put(Context.PROVIDER_URL, CLUSTER2_NODE1.urlOfHttpRemotingConnector);
        final InitialContext ejbCtx_cluster2 = new InitialContext(propertiesCluster2);

        try {
            for (int i1 = 0; i1 < 10; i1++) {
                final WhereAmI beanInCluster2 = (WhereAmI) ejbCtx_cluster2
                        .lookup("ejb:/" + DEPLOYMENT_NAME + "/" + WhereAmIStateful.class.getSimpleName() + "!"
                                + WhereAmI.class.getName() + "?stateful");
                for (int i2 = 0; i2 < 5; i2++) {
                    String nodeInCluster2 = beanInCluster2.getNode();
                    logger.info("Invocation handled by " + nodeInCluster2);
                    Assert.assertTrue("Invocations from this InitialContext should be routed to cluster2, but was routed to " + nodeInCluster2,
                            Arrays.asList(CLUSTER2_NODE1.nodeName, CLUSTER2_NODE2.nodeName).contains(nodeInCluster2));
                }
            }
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx_cluster2);
        }
    }

    @After
    public void cleanup() throws Exception {
        containerController.start(CLUSTER1_NODE1.nodeName);
        containerController.start(CLUSTER1_NODE2.nodeName);
        containerController.start(CLUSTER2_NODE1.nodeName);
        containerController.start(CLUSTER2_NODE2.nodeName);

        DeploymentHelpers.undeploy(deployment.getName(), creaper_cluster1node1);
        DeploymentHelpers.undeploy(deployment.getName(), creaper_cluster1node2);
        DeploymentHelpers.undeploy(deployment.getName(), creaper_cluster2node1);
        DeploymentHelpers.undeploy(deployment.getName(), creaper_cluster2node2);

        // reset the JGroups channels configuration
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster1node1, oldValueOfChannelAttribute);
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster1node2, oldValueOfChannelAttribute);
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster2node1, oldValueOfChannelAttribute);
        ManagementHelpers.setClusterAttributeOfJGroupsEEChannel(creaper_cluster2node2, oldValueOfChannelAttribute);
        new Administration(creaper_cluster1node1).reload();
        new Administration(creaper_cluster1node2).reload();
        new Administration(creaper_cluster2node1).reload();
        new Administration(creaper_cluster2node2).reload();

        ContainerHelpers
                .stopContainers(containerController, CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName,
                        CLUSTER2_NODE1.nodeName, CLUSTER2_NODE2.nodeName);
        setLoggerPrefix("", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
        setLoggerPrefix("", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
        setLoggerPrefix("", CLUSTER2_NODE1.homeDirectory, CLUSTER2_NODE1.configurationXmlFile);
        setLoggerPrefix("", CLUSTER2_NODE2.homeDirectory, CLUSTER2_NODE2.configurationXmlFile);
    }

}