package org.wildfly.ejbclient.testsuite.integration.multinode.transactions;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.NodeAffinity;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.transactions.bean.TransactionalBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.multinode.transactions.bean.TransactionalBeanStateless;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers.isIPv6;

@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
public class TransactionsInCluster_UnspecifiedNodeAffinity_StatelessBean_TestCase {

	private static final JavaArchive deployment = createDeployment();
	private static OnlineManagementClient creaper_cluster1node1;
	private static OnlineManagementClient creaper_cluster1node2;
	@ArquillianResource
	private ContainerController containerController;

	private static Logger logger = Logger.getLogger(TransactionsInCluster_UnspecifiedNodeAffinity_StatelessBean_TestCase.class.getName());

	public static final String TRANSACTIONAL_BEAN_LOOKUP =
			"ejb:/transactions/" + TransactionalBeanStateless.class.getSimpleName() + "!"
					+ TransactionalBeanRemote.class.getName();

	public static JavaArchive createDeployment() {
		final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "transactions.jar");
		jar.addClasses(TransactionalBeanStateless.class, TransactionalBeanRemote.class, Person.class);
		jar.addAsManifestResource(ClassLoader.getSystemResource(
				"org/wildfly/ejbclient/testsuite/integration/multinode/transactions/persistence.xml"), "persistence.xml");
		return jar;
	}

	@Before
	public void before() throws Exception {
		setLoggerPrefix("CLUSTER1_NODE1", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
		setLoggerPrefix("CLUSTER1_NODE2", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);

		containerController.start(CLUSTER1_NODE1.nodeName);
		containerController.start(CLUSTER1_NODE2.nodeName);

		creaper_cluster1node1 = createCreaper(CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.managementPort);
		creaper_cluster1node2 = createCreaper(CLUSTER1_NODE2.bindAddress, CLUSTER1_NODE2.managementPort);

		DeploymentHelpers.deploy(deployment, creaper_cluster1node1);
		DeploymentHelpers.deploy(deployment, creaper_cluster1node2);
	}

	@Test
	public void oneCallPerTransactionAndCommit() throws Exception {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		for (int iteration = 0; iteration < 10; iteration++) {

			final Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final TransactionalBeanRemote bean = (TransactionalBeanRemote) ejbCtx
						.lookup(TRANSACTIONAL_BEAN_LOOKUP);
				final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");

				logger.info("*** BEGIN transaction");
				tx.begin();
				logger.info("*** CREATE person");
				bean.createPerson();
				String node = bean.getNode();
				logger.info("The transaction's affinity is " + node);
				logger.info("*** COMMIT transaction");
				tx.commit();

				// to make sure the next call goes to the same node again (TX end disassociates the affinity)
				EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
				Assert.assertEquals(1, bean.getPersonList().size());
				EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
				bean.clean();
				EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
				Assert.assertEquals(0, bean.getPersonList().size());
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}
	}

	@Test
	public void manyCallsPerTransactionAndCommit() throws Exception {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		for(int iteration = 0; iteration < 10; iteration++) {
			final Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final TransactionalBeanRemote bean = (TransactionalBeanRemote) ejbCtx
						.lookup(TRANSACTIONAL_BEAN_LOOKUP);
				final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");

				String node;
				try {
					tx.begin();
					for (int i = 0; i < 100; i++) {
						bean.createPerson();
					}
					node = bean.getNode();
					logger.info("The transaction's affinity is " + node);
				} finally {
					tx.commit();
				}

				// to make sure the next call goes to the same node again (TX end disassociates the affinity)
				EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
				Assert.assertEquals(100, bean.getPersonList().size());
				EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
				bean.clean();
				EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
				Assert.assertEquals(0, bean.getPersonList().size());
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}
	}

	@Test
	public void oneCallPerTransactionAndRollback() throws Exception {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final TransactionalBeanRemote bean = (TransactionalBeanRemote) ejbCtx
					.lookup(TRANSACTIONAL_BEAN_LOOKUP);
			final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");
			String node;
			try {
				tx.begin();
				bean.createPerson();
				node = bean.getNode();
				logger.info("The transaction's affinity is " + node);
			} finally {
				tx.rollback();
			}

			// to make sure the next call goes to the same node again (TX end disassociates the affinity)
			EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
			Assert.assertEquals(0, bean.getPersonList().size());
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}

	@Test
	public void manyCallsPerTransactionAndRollback() throws Exception {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final TransactionalBeanRemote bean = (TransactionalBeanRemote) ejbCtx
					.lookup(TRANSACTIONAL_BEAN_LOOKUP);
			final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");

			String node = null;
			try {
				tx.begin();
				for (int i = 0; i < 100; i++) {
					bean.createPerson();
					node = bean.getNode();
					logger.info("The transaction's affinity is " + node);
				}
			} finally {
				tx.rollback();
			}

			// to make sure the next call goes to the same node again (TX end disassociates the affinity)
			EJBClient.setStrongAffinity(bean, new NodeAffinity(node));
			Assert.assertEquals(0, bean.getPersonList().size());
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}

	/**
	 * Lazy transactions should be load-balanced, so
	 * different transactions should go to different nodes.
	 */
	@Test
	public void testLoadBalancingOfTransactions() throws Exception {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

		Map<String, Integer> nodesToInvocationsStatistics = new HashMap<>();
		for(int iteration = 0; iteration < 100; iteration++) {
			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final TransactionalBeanRemote bean = (TransactionalBeanRemote) ejbCtx
						.lookup(TRANSACTIONAL_BEAN_LOOKUP);
				final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");
				tx.begin();
				nodesToInvocationsStatistics.merge(bean.getNode(), 1, (oldValue, one) -> oldValue + one);
				tx.rollback();                               
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}

		// assert that each node was picked at least once
		Assert.assertTrue("each node should be invoked at least once",
				nodesToInvocationsStatistics.getOrDefault(CLUSTER1_NODE1.nodeName, 0) > 0);
		Assert.assertTrue("each node should be invoked at least once",
				nodesToInvocationsStatistics.getOrDefault(CLUSTER1_NODE2.nodeName, 0) > 0);
	}

	/**
	 * When there are two bean proxies looked up and there is a transaction running,
	 * invocations on both proxies should be routed to the same node.
	 */
	@Test
	public void twoBeanInstances_thenCommit() throws NamingException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final TransactionalBeanRemote bean1 = (TransactionalBeanRemote) ejbCtx
					.lookup(TRANSACTIONAL_BEAN_LOOKUP);
			final TransactionalBeanRemote bean2 = (TransactionalBeanRemote) ejbCtx
					.lookup(TRANSACTIONAL_BEAN_LOOKUP);
			final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");
			String nodeOfFirstInvocation;
			String nodeOfSecondInvocation;
			try {
				tx.begin();
				bean1.createPerson();
				nodeOfFirstInvocation = bean1.getNode();
				bean2.createPerson();
				nodeOfSecondInvocation = bean2.getNode();
				logger.info("The transaction's affinity is " + nodeOfFirstInvocation);
			} finally {
				tx.commit();
			}
			// make sure the invocations for bean1 and bean2 were routed to the same node
			Assert.assertEquals("bean1 and bean2 invocations should be routed to the same node",
					nodeOfFirstInvocation, nodeOfSecondInvocation);

			// to make sure the next call goes to the same node again (TX end disassociates the affinity)
			EJBClient.setStrongAffinity(bean1, new NodeAffinity(nodeOfFirstInvocation));
			Assert.assertEquals(2, bean1.getPersonList().size());
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}

	/**
	 * When there are two bean proxies looked up and there is a transaction running,
	 * invocations on both proxies should be routed to the same node.
	 */
	@Test
	public void twoBeanInstances_thenRollback() throws NamingException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
		setWildflyConfigXml("wildfly-config-cluster1-node1");
		final Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());

		final InitialContext ejbCtx = new InitialContext(properties);
		try {
			final TransactionalBeanRemote bean1 = (TransactionalBeanRemote) ejbCtx
					.lookup(TRANSACTIONAL_BEAN_LOOKUP);
			final TransactionalBeanRemote bean2 = (TransactionalBeanRemote) ejbCtx
					.lookup(TRANSACTIONAL_BEAN_LOOKUP);
			final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:RemoteUserTransaction");
			String nodeOfFirstInvocation;
			String nodeOfSecondInvocation;
			try {
				tx.begin();
				bean1.createPerson();
				nodeOfFirstInvocation = bean1.getNode();
				bean2.createPerson();
				nodeOfSecondInvocation = bean2.getNode();
				logger.info("The transaction's affinity is " + nodeOfFirstInvocation);
			} finally {
				tx.rollback();
			}
			// make sure the invocations for bean1 and bean2 were routed to the same node
			Assert.assertEquals("bean1 and bean2 invocations should be routed to the same node",
					nodeOfFirstInvocation, nodeOfSecondInvocation);

			// to make sure the next call goes to the same node again (TX end disassociates the affinity)
			EJBClient.setStrongAffinity(bean1, new NodeAffinity(nodeOfFirstInvocation));
			Assert.assertEquals(0, bean1.getPersonList().size());
		} finally {
			MiscHelpers.safeCloseEjbClientContext(ejbCtx);
		}
	}


	@After
	public void cleanup() throws Exception {
		containerController.start(CLUSTER1_NODE1.nodeName);
		containerController.start(CLUSTER1_NODE2.nodeName);

		DeploymentHelpers.undeploy(deployment.getName(), creaper_cluster1node1);
		DeploymentHelpers.undeploy(deployment.getName(), creaper_cluster1node2);

		containerController.stop(CLUSTER1_NODE1.nodeName);
		containerController.stop(CLUSTER1_NODE2.nodeName);

		setLoggerPrefix("", CLUSTER1_NODE1.homeDirectory, CLUSTER1_NODE1.configurationXmlFile);
		setLoggerPrefix("", CLUSTER1_NODE2.homeDirectory, CLUSTER1_NODE2.configurationXmlFile);
	}

	public void setWildflyConfigXml(String name) {
		if ( !isIPv6() ) {
			System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
					"org/wildfly/ejbclient/testsuite/integration/multinode/transactions/" + name + ".xml").toString());
		} else {
			System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
					"org/wildfly/ejbclient/testsuite/integration/multinode/transactions/" + name + "-ipv6.xml").toString());
		}
	}


}
