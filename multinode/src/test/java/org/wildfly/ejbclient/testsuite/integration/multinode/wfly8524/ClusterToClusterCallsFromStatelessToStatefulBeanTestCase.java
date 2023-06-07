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

package org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524;

import java.io.IOException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans.IntermediaryBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans.IntermediaryBeanStateless;
import org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans.TargetBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans.TargetBeanStateful;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER2_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER2_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createRemoteOutboundConnection;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.removeRemoteOutboundConnection;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers.isIPv6;

/**
 * Call stateless bean in cluster1 which then calls a stateful bean in cluster2 (over a remote outbound connection).
 * Do this a lot of times in a row.
 * See https://issues.redhat.com/browse/WFLY-8524
 */
@SuppressWarnings("ArquillianDeploymentAbsent")
@RunWith(Arquillian.class)
@RunAsClient
public class ClusterToClusterCallsFromStatelessToStatefulBeanTestCase {

    private static final JavaArchive deploymentTarget = createDeploymentTarget();
    private static final JavaArchive deploymentIntermediary = createDeploymentIntermediary();
    private static OnlineManagementClient creaper_cluster1node1;
    private static OnlineManagementClient creaper_cluster1node2;
    private static OnlineManagementClient creaper_cluster2node1;
    private static OnlineManagementClient creaper_cluster2node2;
    @ArquillianResource
    private ContainerController containerController;

    /**
     * The deployment for cluster2 (the "target" one).
     */
    public static JavaArchive createDeploymentTarget() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bean-target.jar");
        jar.addClasses(TargetBeanRemote.class, TargetBeanStateful.class);
        return jar;
    }

    /**
     * The deployment for cluster1 (the "intermediary" one).
     */
    public static JavaArchive createDeploymentIntermediary() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bean-intermediary.jar");
        jar.addClasses(IntermediaryBeanRemote.class, IntermediaryBeanStateless.class, TargetBeanRemote.class);
        jar.addAsManifestResource(ClassLoader.getSystemResource(
                "org/wildfly/ejbclient/testsuite/integration/multinode/wfly8524/jboss-ejb-client.xml"), "jboss-ejb-client.xml");
        return jar;
    }

    @BeforeClass
    public static void prepare() throws IOException {
        if (isIPv6()) {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
                    "org/wildfly/ejbclient/testsuite/integration/multinode/wfly8524/wildfly-config-wfly8524-ipv6.xml").toString());
        } else {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
                    "org/wildfly/ejbclient/testsuite/integration/multinode/wfly8524/wildfly-config-wfly8524.xml").toString());
        }
    }

    @Before
    public void before() throws Exception {
        setLoggerPrefix("CLUSTER1_NODE1", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
        setLoggerPrefix("CLUSTER1_NODE2", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
        setLoggerPrefix("CLUSTER2_NODE1", CLUSTER2_NODE1.homeDirectory, CLUSTER2_NODE1.configurationXmlFile);
        setLoggerPrefix("CLUSTER2_NODE2", CLUSTER2_NODE2.homeDirectory, CLUSTER2_NODE2.configurationXmlFile);

        containerController.start(CLUSTER1_NODE1.nodeName);
        containerController.start(CLUSTER1_NODE2.nodeName);
        containerController.start(CLUSTER2_NODE1.nodeName);
        containerController.start(CLUSTER2_NODE2.nodeName);

        creaper_cluster1node1 = createCreaper(CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.managementPort);
        creaper_cluster1node2 = createCreaper(CLUSTER1_NODE2.bindAddress, CLUSTER1_NODE2.managementPort);
        creaper_cluster2node1 = createCreaper(CLUSTER2_NODE1.bindAddress, CLUSTER2_NODE1.managementPort);
        creaper_cluster2node2 = createCreaper(CLUSTER2_NODE2.bindAddress, CLUSTER2_NODE2.managementPort);

        createRemoteOutboundConnection(creaper_cluster1node1, "connection-to-cluster2",
                CLUSTER2_NODE1.bindAddress, CLUSTER2_NODE1.applicationPort, null);
        createRemoteOutboundConnection(creaper_cluster1node2, "connection-to-cluster2",
                CLUSTER2_NODE1.bindAddress, CLUSTER2_NODE1.applicationPort, null);

        DeploymentHelpers.deploy(deploymentIntermediary, creaper_cluster1node1);
        DeploymentHelpers.deploy(deploymentIntermediary, creaper_cluster1node2);
        DeploymentHelpers.deploy(deploymentTarget, creaper_cluster2node1);
        DeploymentHelpers.deploy(deploymentTarget, creaper_cluster2node2);
    }

    @Test
    public void doTest() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        final IntermediaryBeanRemote bean = (IntermediaryBeanRemote)ejbCtx
                .lookup("ejb:/bean-intermediary/" + IntermediaryBeanStateless.class.getSimpleName() + "!"
                        + IntermediaryBeanRemote.class.getName());
        for (int i = 0; i < 200; i++) {
            bean.call();
        }
    }

    @After
    public void cleanup() throws Exception {
        DeploymentHelpers.undeploy(deploymentIntermediary.getName(), creaper_cluster1node1);
        DeploymentHelpers.undeploy(deploymentIntermediary.getName(), creaper_cluster1node2);
        DeploymentHelpers.undeploy(deploymentTarget.getName(), creaper_cluster2node1);
        DeploymentHelpers.undeploy(deploymentTarget.getName(), creaper_cluster2node2);

        removeRemoteOutboundConnection(creaper_cluster1node1, "connection-to-cluster2");
        removeRemoteOutboundConnection(creaper_cluster1node2, "connection-to-cluster2");

        containerController.stop(CLUSTER1_NODE1.nodeName);
        containerController.stop(CLUSTER1_NODE2.nodeName);
        containerController.stop(CLUSTER2_NODE1.nodeName);
        containerController.stop(CLUSTER2_NODE2.nodeName);

        setLoggerPrefix("", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
        setLoggerPrefix("", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
        setLoggerPrefix("", CLUSTER2_NODE1.homeDirectory, CLUSTER2_NODE1.configurationXmlFile);
        setLoggerPrefix("", CLUSTER2_NODE2.homeDirectory, CLUSTER2_NODE2.configurationXmlFile);
    }


}
