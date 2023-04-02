package org.wildfly.ejbclient.testsuite.integration.multinode.clusternodeselector;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whereami.WhereAmI;
import org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whereami.WhereAmIStateless;
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
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

/**
 * Verify that it is possible to use a custom cluster node selector.
 */
@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class ClusterNodeSelectorTestCase {

	private static final JavaArchive deployment = createDeployment();
	private static OnlineManagementClient creaper_node1;
	private static OnlineManagementClient creaper_node2;


	@ArquillianResource
	private ContainerController containerController;

	private static Logger logger = Logger.getLogger(ClusterNodeSelectorTestCase.class.getName());

	public static JavaArchive createDeployment() {
		final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cluster-node-selector.jar");
		jar.addClasses(WhereAmI.class, WhereAmIStateless.class);
		return jar;
	}

	@BeforeClass
	public static void prepare() throws IOException {
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
	 * We have a cluster node selector which accepts "hints" from the test
	 * The hint tells the selector what node should be selected.
	 *
	 * This test gives hints to the selector and then verifies
	 * that the next invocation is really routed to the requested node.
	 */
	@Test
	public void testCustomClusterNodeSelector() throws Exception {
		final EJBClientContext.Builder builder = new EJBClientContext.Builder()
				.setClusterNodeSelector(new CustomClusterNodeSelector())
				.addTransportProvider(new RemoteTransportProvider())
				.addClientConnection(
						new EJBClientConnection.Builder()
								.setDestination(URI.create(CLUSTER1_NODE1.urlOfHttpRemotingConnector))
								.setForDiscovery(true)
								.build()
				);
		final EJBClientContext ejbClientContext = builder.build();

		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
		final InitialContext ejbCtx = new InitialContext(properties);

		ejbClientContext.run(() -> {
			try {
				final WhereAmI bean = (WhereAmI) ejbCtx
						.lookup("ejb:/cluster-node-selector/" + WhereAmIStateless.class.getSimpleName() + "!"
								+ WhereAmI.class.getName());
				for(int i = 0; i < 50; i++) {
					CustomClusterNodeSelector.setNodeHint(CLUSTER1_NODE1.nodeName);
					Assert.assertEquals(CLUSTER1_NODE1.nodeName, bean.getNode());
					CustomClusterNodeSelector.setNodeHint(CLUSTER1_NODE2.nodeName);
					Assert.assertEquals(CLUSTER1_NODE2.nodeName, bean.getNode());
				}
			} catch (NamingException e) {
				throw new RuntimeException(e);
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		});
	}


	@After
	public void cleanup() throws Exception {
		DeploymentHelpers.undeploy(deployment.getName(), creaper_node1);
		DeploymentHelpers.undeploy(deployment.getName(), creaper_node2);
		ContainerHelpers
				.stopContainers(containerController, CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName);
		setLoggerPrefix("", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
		setLoggerPrefix("", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
	}

}