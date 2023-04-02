package org.wildfly.ejbclient.testsuite.integration.multinode.contextdata;

import java.io.IOException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createRemoteOutboundConnection;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.removeRemoteOutboundConnection;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

/**
 * Test for passing EJB context data in server-to-server scenarios
 */
@SuppressWarnings("ArquillianDeploymentAbsent")
@RunWith(Arquillian.class)
public class ContextDataPassingBetweenServersTestCase {

    private static final JavaArchive deploymentTarget = createDeploymentTarget();
    private static final JavaArchive deploymentIntermediary = createDeploymentIntermediary();
    private static OnlineManagementClient creaper_node1;
    private static OnlineManagementClient creaper_node2;
    private final String CONTEXT_DATA_KEY = "gagagalalala";
    private final Long CONTEXT_DATA_VALUE = 42L;
    @ArquillianResource
    private ContainerController containerController;

    /**
     * The deployment with the "target" bean
     */
    public static JavaArchive createDeploymentTarget() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bean-target.jar");
        jar.addClasses(TargetBeanReturningContextDataRemote.class, TargetBeanReturningContextData.class);
        return jar;
    }

    /**
     * The deployment for the "intermediary" bean
     */
    public static JavaArchive createDeploymentIntermediary() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bean-intermediary.jar");
        jar.addClasses(IntermediaryBeanAddingContextData.class,
                IntermediaryBeanAddingContextDataRemote.class,
                TargetBeanReturningContextDataRemote.class);
        jar.addAsManifestResource(ClassLoader.getSystemResource(
                "org/wildfly/ejbclient/testsuite/integration/multinode/contextdata/jboss-ejb-client.xml"), "jboss-ejb-client.xml");
        return jar;
    }

    @Before
    public void before() throws IOException, CommandFailedException {
        setLoggerPrefix("NODE1", NODE1.homeDirectory, NODE1.configurationXmlFile);
        setLoggerPrefix("NODE2", NODE2.homeDirectory, NODE2.configurationXmlFile);

        containerController.start(NODE1.nodeName);
        containerController.start(NODE2.nodeName);

        creaper_node1 = createCreaper(NODE1.bindAddress, NODE1.managementPort);
        creaper_node2 = createCreaper(NODE2.bindAddress, NODE2.managementPort);

        createRemoteOutboundConnection(creaper_node1, "connection-to-node2",
                NODE2.bindAddress, NODE2.applicationPort, null);

        DeploymentHelpers.deploy(deploymentIntermediary, creaper_node1);
        DeploymentHelpers.deploy(deploymentTarget, creaper_node2);
    }

    /**
     * Scenario:
     * - client invokes IntermediaryBeanAddingContextData (deployed on node1) and passes an object which should be added as context data
     * - IntermediaryBeanAddingContextData invokes TargetBeanReturningContextData (deployed on node2)
     * and adds the requested context data to the invocation
     * - the call to TargetBeanReturningContextData takes the context data and returns it (as regular return value from method)
     * - IntermediaryBeanAddingContextData passes the returned value back to the client, so the client should receive the value of the context data
     */
    @Test
    public void performTest() throws Exception {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        properties.put(Context.PROVIDER_URL, NODE1.urlOfHttpRemotingConnector);
        final InitialContext ejbCtx = new InitialContext(properties);
        try {
            final IntermediaryBeanAddingContextDataRemote bean
                    = (IntermediaryBeanAddingContextDataRemote)ejbCtx
                    .lookup("ejb:/bean-intermediary/" + IntermediaryBeanAddingContextData.class
                            .getSimpleName() + "!"
                            + IntermediaryBeanAddingContextDataRemote.class.getName());
            final Object result = bean.addContextDataCallAndReturnIt(CONTEXT_DATA_KEY, CONTEXT_DATA_VALUE);
            Assert.assertEquals("Unexpected context data returned", CONTEXT_DATA_VALUE, result);
        } finally {
            MiscHelpers.safeCloseEjbClientContext(ejbCtx);
        }
    }

    @After
    public void cleanup() throws Exception {
        DeploymentHelpers.undeploy(deploymentIntermediary.getName(), creaper_node1);
        DeploymentHelpers.undeploy(deploymentTarget.getName(), creaper_node2);

        removeRemoteOutboundConnection(creaper_node1, "connection-to-cluster2");

        containerController.stop(NODE1.nodeName);
        containerController.stop(NODE2.nodeName);

        setLoggerPrefix("", NODE1.homeDirectory, NODE1.configurationXmlFile);
        setLoggerPrefix("", NODE2.homeDirectory, NODE2.configurationXmlFile);
    }

}
