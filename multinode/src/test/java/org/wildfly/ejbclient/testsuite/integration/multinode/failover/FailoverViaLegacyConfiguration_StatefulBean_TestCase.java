package org.wildfly.ejbclient.testsuite.integration.multinode.failover;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ContainerHelpers;
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
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

/**
 * @author Michal Vinkler
 */
@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class FailoverViaLegacyConfiguration_StatefulBean_TestCase {

    private static final JavaArchive deployment = createDeployment();
    private static OnlineManagementClient creaper_node1;
    private static OnlineManagementClient creaper_node2;
    @ArquillianResource
    private ContainerController containerController;

    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "basic-failover-stateful.jar");
        jar.addClasses(CounterBeanStateful.class, CounterBean.class);
        return jar;
    }

    @BeforeClass
    public static void prepare() throws IOException {
        System.setProperty("jboss.ejb.client.properties.file.path", ClassLoader.getSystemResource(
                "org/wildfly/ejbclient/testsuite/integration/multinode/failover/jboss-ejb-client.properties").getPath());
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
     * - Obtain a stateful EJB proxy using legacy jboss-ejb-client.properties EJB client configuration
     * - Invoke the EJB
     * - Undeploy the app on node1
     * - Invoke the EJB, assert that it was handled by node2 and the state was preserved
     * - Deploy the app on node1 again
     * - Undeploy the app on node2
     * - Invoke the EJB, assert that it was handled by node1 and the state was preserved
     */
    @Test
    public void testSimpleFailover_undeploy() throws Exception {
        final Properties properties = new Properties();
        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            String lookup = "ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                    + CounterBean.class.getName() + "?stateful";
            final CounterBean bean = (CounterBean) ejbCtx
                    .lookup(lookup);

            //EJBClient.setStrongAffinity(bean, new ClusterAffinity("ejb")); //FIXME: use this workaround if needed

            String node = bean.getNode();
            Assert.assertTrue("Unexpected node handled the invocation: " + node,
                    Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(node));
            int counter = bean.getCounterAndIncrement();
            Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 1, counter);
            DeploymentHelpers.undeploy(deployment.getName(), creaper_node1);
            Assert.assertEquals("should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
            counter = bean.getCounterAndIncrement();
            Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 2, counter);
            DeploymentHelpers.deploy(deployment, creaper_node1);
            DeploymentHelpers.undeploy(deployment.getName(), creaper_node2);
            Assert.assertEquals("should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
            counter = bean.getCounterAndIncrement();
            Assert.assertEquals("Unexpected counter value returned by stateful bean: ", 3, counter);
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx);
        }
    }

    /**
     * - Run two cluster nodes
     * - Obtain a stateful EJB proxy using legacy jboss-ejb-client.properties EJB client configuration
     * - Invoke the EJB
     * - Stop node1
     * - Invoke the EJB, assert that it was handled by node2 and the state was preserved
     * - Start node1 again
     * - Stop node2
     * - Invoke the EJB, assert that it was handled by node1 and the state was preserved
     */
    @Test
    public void testSimpleFailover_shutdown() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            final CounterBean bean = (CounterBean) ejbCtx
                    .lookup("ejb:/basic-failover-stateful/" + CounterBeanStateful.class.getSimpleName() + "!"
                            + CounterBean.class.getName() + "?stateful");

            //EJBClient.setStrongAffinity(bean, new ClusterAffinity("ejb")); //FIXME: use this workaround if needed

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
