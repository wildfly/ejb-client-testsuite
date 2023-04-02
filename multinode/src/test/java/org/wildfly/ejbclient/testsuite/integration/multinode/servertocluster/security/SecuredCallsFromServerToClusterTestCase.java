package org.wildfly.ejbclient.testsuite.integration.multinode.servertocluster.security;

import java.io.IOException;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
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
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createAuthenticationContext;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createRemoteOutboundConnection;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.removeAuthenticationContext;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.removeRemoteOutboundConnection;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.SecurityHelpers.createTestingUsers;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.SecurityHelpers.removeTestingUsers;

@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class SecuredCallsFromServerToClusterTestCase {

	private static final JavaArchive deploymentIntermediary = createDeploymentIntermediary();
	private static final JavaArchive deploymentSecured = createDeploymentSecured();


	public static final String DEPLOYMENT_NAME_INTERMEDIARY = "intermediary";
	public static final String DEPLOYMENT_NAME_SECURED = "secured";

	private static OnlineManagementClient creaper_node1;
	private static OnlineManagementClient creaper_cluster1node1;
	private static OnlineManagementClient creaper_cluster1node2;

	private static Logger logger = Logger.getLogger(SecuredCallsFromServerToClusterTestCase.class.getName());

	@ArquillianResource
	private ContainerController containerController;

	public static JavaArchive createDeploymentIntermediary() {
		final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME_INTERMEDIARY + ".jar");
		jar.addClasses(IntermediaryBean.class, IntermediaryBeanRemote.class, SecuredBeanRemote.class);
		jar.addAsManifestResource(ClassLoader.getSystemResource(
				"org/wildfly/ejbclient/testsuite/integration/multinode/servertocluster/security/jboss-ejb-client.xml"), "jboss-ejb-client.xml");
		return jar;
	}

	public static JavaArchive createDeploymentSecured() {
		final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME_SECURED + ".jar");
		jar.addClasses(SecuredBean.class, SecuredBeanRemote.class);
		return jar;
	}

	@BeforeClass
	public static void prepare() throws IOException {
		creaper_node1 = ManagementHelpers
				.createCreaper(NODE1.bindAddress, NODE1.managementPort);
		creaper_cluster1node1 = ManagementHelpers
				.createCreaper(CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.managementPort);
		creaper_cluster1node2 = ManagementHelpers
				.createCreaper(CLUSTER1_NODE2.bindAddress, CLUSTER1_NODE2.managementPort);
	}

	@AfterClass
	public static void afterClass() {
		ManagementHelpers.safeClose(creaper_node1, creaper_cluster1node1, creaper_cluster1node2);
	}

	@Before
	public void start() throws Exception {
		setLoggerPrefix("NODE1", NODE1.homeDirectory, NODE1.configurationXmlFile);
		setLoggerPrefix("CLUSTER1_NODE1", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
		setLoggerPrefix("CLUSTER1_NODE2", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
		createTestingUsers(CLUSTER1_NODE1.homeDirectory);
		createTestingUsers(CLUSTER1_NODE2.homeDirectory);
		containerController.start(NODE1.nodeName);
		containerController.start(CLUSTER1_NODE1.nodeName);
		containerController.start(CLUSTER1_NODE2.nodeName);

		createAuthenticationContext(creaper_node1, "connection-to-cluster1-authentication", "joe", "joeIsAwesome2013!");
		createRemoteOutboundConnection(creaper_node1, "connection-to-cluster1",
				CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.applicationPort, "connection-to-cluster1-authentication");

		DeploymentHelpers.deploy(deploymentIntermediary, creaper_node1);
		DeploymentHelpers.deploy(deploymentSecured, creaper_cluster1node1);
		DeploymentHelpers.deploy(deploymentSecured, creaper_cluster1node2);
	}


	/**
	 * Scenario:
	 * Bean deployed on node1 calls a secured bean deployed in cluster1 (cluster1-node1 and cluster1-node2)
	 * The remote outbound connection on node1 is configured against cluster1-node1 only, cluster1-node2 should be discovered.
	 *
	 * Expected outcome:
	 * The authentication-context configured for the remote outbound connection has to
	 * 		be applied even when the invocation is routed to cluster1-node2!
	 */
	@Test
	public void testSecuredClusterInvocationsViaRemoteOutboundConnectionWithLoadBalancingEnabled() throws Exception {
		final Properties propertiesCluster1 = new Properties();
		propertiesCluster1.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
		propertiesCluster1.put(Context.PROVIDER_URL, NODE1.urlOfHttpRemotingConnector);
		final InitialContext ejbCtx_node1 = new InitialContext(propertiesCluster1);

		try {
			final IntermediaryBeanRemote intermediaryBean = (IntermediaryBeanRemote) ejbCtx_node1
					.lookup("ejb:/" + DEPLOYMENT_NAME_INTERMEDIARY + "/" + IntermediaryBean.class.getSimpleName() + "!"
							+ IntermediaryBeanRemote.class.getName());
			boolean cluster1Node1WasCalled = false;
			boolean cluster1Node2WasCalled = false;
			for (int i = 0; i < 100; i++) {
				final String nameAndNode = intermediaryBean.callRemoteSecuredBean();
				logger.info(nameAndNode);
				Assert.assertTrue("Unexpected response: " + nameAndNode,
						nameAndNode.equals("joe@cluster1-node1") ||  nameAndNode.equals("joe@cluster1-node2"));
				if(nameAndNode.equals("joe@cluster1-node1"))
					cluster1Node1WasCalled = true;
				if(nameAndNode.equals("joe@cluster1-node2"))
					cluster1Node2WasCalled = true;
			}
			if(!cluster1Node1WasCalled)
				Assert.fail("Sanity check failed, cluster1-node1 should be called at least once due to load balancing");
			if(!cluster1Node2WasCalled)
				Assert.fail("Sanity check failed, cluster1-node2 should be called at least once due to load balancing");
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx_node1);
		}
	}

	@After
	public void cleanup() throws Exception {
		containerController.start(NODE1.nodeName);
		containerController.start(CLUSTER1_NODE1.nodeName);
		containerController.start(CLUSTER1_NODE2.nodeName);

		DeploymentHelpers.undeploy(deploymentIntermediary.getName(), creaper_node1);
		DeploymentHelpers.undeploy(deploymentSecured.getName(), creaper_cluster1node1);
		DeploymentHelpers.undeploy(deploymentSecured.getName(), creaper_cluster1node2);

		removeRemoteOutboundConnection(creaper_node1, "connection-to-cluster1");
		removeAuthenticationContext(creaper_node1, "connection-to-cluster1-authentication");

		ContainerHelpers
				.stopContainers(containerController, NODE1.nodeName, CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName);
		setLoggerPrefix("", NODE1.homeDirectory, NODE1.configurationXmlFile);
		setLoggerPrefix("", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
		setLoggerPrefix("", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);

		removeTestingUsers(CLUSTER1_NODE1.homeDirectory);
		removeTestingUsers(CLUSTER1_NODE2.homeDirectory);
	}

}