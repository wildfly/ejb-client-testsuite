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

package org.wildfly.ejbclient.testsuite.integration.multinode.transactions;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.transactions.bean.TransactionalBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.multinode.transactions.bean.TransactionalBeanStateful;
import org.wildfly.ejbclient.testsuite.integration.multinode.transactions.bean.TransactionalBeanStateless;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE2;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;

@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("FIXME - I need to look more into this, it fails with stateful beans " +
		"but I'm not sure if that is a bug or " +
		"if the test is nonsense")
public class TransactionAttributes_StatelessBean_TestCase {

	private static final JavaArchive deployment = createDeployment();
	private static OnlineManagementClient creaper_cluster1node1;
	private static OnlineManagementClient creaper_cluster1node2;
	
	@ArquillianResource
	private ContainerController containerController;

	public static final String STATELESS_BEAN_LOOKUP =
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
	public void begin_mandatoryMethod_notsupportedMethod_commit() throws Exception {
		for(int iteration = 0; iteration < 10; iteration++) {

			final Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
			properties.put(Context.PROVIDER_URL, CLUSTER1_NODE1.urlOfHttpRemotingConnector);

			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final TransactionalBeanRemote bean = (TransactionalBeanRemote) ejbCtx
						.lookup(STATELESS_BEAN_LOOKUP);
				final UserTransaction tx = (UserTransaction) ejbCtx.lookup("txn:UserTransaction");
				try {
					tx.begin();
					bean.createPerson(); // MANDATORY
					bean.dummyMethodWithNotSupportedTransactions();    // NOT_SUPPORTED
				} finally {
					tx.commit();
				}
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
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


}
