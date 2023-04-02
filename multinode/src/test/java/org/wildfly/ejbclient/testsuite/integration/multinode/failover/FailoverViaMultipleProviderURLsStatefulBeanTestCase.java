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
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

/**
 * see EJBCLIENT-273
 */

@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class FailoverViaMultipleProviderURLsStatefulBeanTestCase {

	private static final JavaArchive deployment = createDeployment();
	private static OnlineManagementClient creaper_node1;
	private static OnlineManagementClient creaper_node2;

	private static Logger logger = Logger.getLogger(FailoverViaMultipleProviderURLsStatefulBeanTestCase.class.getName());

	@ArquillianResource
	private ContainerController containerController;

	public static JavaArchive createDeployment() {
		final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "basic-failover.jar");
		jar.addClasses(CounterBean.class, CounterBeanStateful.class);
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
	 * - Run two cluster nodes
	 * - Obtain an EJB proxy
	 * - Repeat X times:
	 * -- Invoke the EJB
	 * -- Undeploy the app on node1
	 * -- Invoke the EJB, assert that it was handled by node2
	 * -- Deploy the app on node1 again
	 * -- Undeploy the app on node2
	 * -- Invoke the EJB, assert that it was handled by node1
	 */
	@Test
	public void testFailover_undeploy() throws Exception {
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
		properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector
				+ "," + CLUSTER1_NODE2.urlOfHttpRemotingConnector);
		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final CounterBean bean = (CounterBean) ejbCtx
					.lookup("ejb:/basic-failover/" + CounterBeanStateful.class.getSimpleName() + "!"
							+ CounterBean.class.getName() + "?stateful");

			for (int iteration = 0; iteration < 20; iteration++) {
				logger.info(String.format("starting iteration %d/20", iteration));

				String node = bean.getNode();
				Assert.assertTrue("Unexpected node handled the invocation: " + node,
						Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(node));
				DeploymentHelpers.undeploy(deployment.getName(), creaper_node1);
				Assert.assertEquals("should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
				DeploymentHelpers.deploy(deployment, creaper_node1);
				DeploymentHelpers.undeploy(deployment.getName(), creaper_node2);
				Assert.assertEquals("should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
				DeploymentHelpers.deploy(deployment, creaper_node2);
			}
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}

	/**
	 * - Run two cluster nodes
	 * - Obtain an EJB proxy
	 * repeat X times:
	 * -- Invoke the EJB
	 * -- Stop node1
	 * -- Invoke the EJB, assert that it was handled by node2
	 * -- Start node1 again
	 * -- Stop node2
	 * -- Invoke the EJB, assert that it was handled by node1
	 * -- Start node2 again
	 */
	@Test
	public void testFailover_shutdown() throws NamingException {
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
		properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector
				+ "," + CLUSTER1_NODE2.urlOfHttpRemotingConnector);
		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final CounterBean bean = (CounterBean) ejbCtx
					.lookup("ejb:/basic-failover/" + CounterBeanStateful.class.getSimpleName() + "!"
							+ CounterBean.class.getName() + "?stateful");

			for (int iteration = 0; iteration < 5; iteration++) {
				logger.info(String.format("starting iteration %d/5", iteration));

				String node = bean.getNode();
				Assert.assertTrue("Unexpected node handled the invocation: " + node,
						Arrays.asList(CLUSTER1_NODE1.nodeName, CLUSTER1_NODE2.nodeName).contains(node));
				containerController.stop(CLUSTER1_NODE1.nodeName);
				Assert.assertEquals("should invoke on " + CLUSTER1_NODE2, CLUSTER1_NODE2.nodeName, bean.getNode());
				containerController.start(CLUSTER1_NODE1.nodeName);
				containerController.stop(CLUSTER1_NODE2.nodeName);
				try {
					// to prevent intermittent failures- it can take the client some time to update the topology
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Assert.assertEquals("should invoke on " + CLUSTER1_NODE1, CLUSTER1_NODE1.nodeName, bean.getNode());
				containerController.start(CLUSTER1_NODE2.nodeName);
			}
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}

	/**
	 * When just one server is running and reachable, and the PROVIDER_URL contains
	 * URLs to two different servers, it should try to use the reachable server.
	 */

	@Test
	public void justOneServerRunning_theFirstUrlWorks() throws Exception {
		ContainerHelpers.stopContainers(containerController, CLUSTER1_NODE2.nodeName);

		for (int iteration = 0; iteration < 20; iteration++) {
			System.out.printf("Starting iteration %d/20\n", iteration);

			final Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
			properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector
					+ "," + CLUSTER1_NODE2.urlOfHttpRemotingConnector);
			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final CounterBean bean = (CounterBean) ejbCtx
						.lookup("ejb:/basic-failover/" + CounterBeanStateful.class.getSimpleName() + "!"
								+ CounterBean.class.getName() + "?stateful");

				for (int i = 0; i < 15; i++) {
					System.out.println("Trying invocation number :" + i + "/15");
					String node = bean.getNode();
					Assert.assertTrue("Unexpected node handled the invocation: " + node, node.equals(CLUSTER1_NODE1.nodeName));
				}
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}
	}

	/**
	 * When just one server is running and reachable, and the PROVIDER_URL contains
	 * URLs to two different servers, it should try to use the reachable server.
	 */
	@Test
	public void justOneServerRunning_theSecondUrlWorks() throws Exception {
		ContainerHelpers.stopContainers(containerController, CLUSTER1_NODE1.nodeName);

		for (int iteration = 0; iteration < 20; iteration++) {
			System.out.printf("Starting iteration %d/20\n", iteration);

			final Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
			properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector
					+ "," + CLUSTER1_NODE2.urlOfHttpRemotingConnector);
			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final CounterBean bean = (CounterBean) ejbCtx
						.lookup("ejb:/basic-failover/" + CounterBeanStateful.class.getSimpleName() + "!"
								+ CounterBean.class.getName() + "?stateful");

				for (int i = 0; i < 15; i++) {
					System.out.println("Trying invocation number :" + i + "/15");
					String node = bean.getNode();
					Assert.assertTrue("Unexpected node handled the invocation: " + node, node.equals(CLUSTER1_NODE2.nodeName));
				}
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}
	}

	@Test
	public void node1WorksFirst_ThenRunNode2AndStopNode1() throws Exception {
		ContainerHelpers.stopContainers(containerController, CLUSTER1_NODE2.nodeName);

		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
		properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector
				+ "," + CLUSTER1_NODE2.urlOfHttpRemotingConnector);
		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final CounterBean bean = (CounterBean) ejbCtx
					.lookup("ejb:/basic-failover/" + CounterBeanStateful.class.getSimpleName() + "!"
							+ CounterBean.class.getName() + "?stateful");

			for (int i = 0; i < 15; i++) {
				System.out.println("Trying invocation number :" + i + "/15");
				String node = bean.getNode();
				Assert.assertTrue("Unexpected node handled the invocation: " + node, node.equals(CLUSTER1_NODE1.nodeName));
			}

			containerController.start(CLUSTER1_NODE2.nodeName);
			containerController.stop(CLUSTER1_NODE1.nodeName);

			for (int i = 0; i < 15; i++) {
				String node = bean.getNode();
				Assert.assertTrue("Unexpected node handled the invocation: " + node, node.equals(CLUSTER1_NODE2.nodeName));
			}
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}

	@Test
	public void node2WorksFirst_ThenRunNode1AndStopNode2() throws Exception {
		ContainerHelpers.stopContainers(containerController, CLUSTER1_NODE1.nodeName);

		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
		properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector
				+ "," + CLUSTER1_NODE2.urlOfHttpRemotingConnector);
		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final CounterBean bean = (CounterBean) ejbCtx
					.lookup("ejb:/basic-failover/" + CounterBeanStateful.class.getSimpleName() + "!"
							+ CounterBean.class.getName() + "?stateful");

			for (int i = 0; i < 15; i++) {
				System.out.println("Trying invocation number :" + i + "/15");
				String node = bean.getNode();
				Assert.assertTrue("Unexpected node handled the invocation: " + node, node.equals(CLUSTER1_NODE2.nodeName));
			}

			containerController.start(CLUSTER1_NODE1.nodeName);
			containerController.stop(CLUSTER1_NODE2.nodeName);

			for (int i = 0; i < 15; i++) {
				String node = bean.getNode();
				Assert.assertTrue("Unexpected node handled the invocation: " + node, node.equals(CLUSTER1_NODE1.nodeName));
			}
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