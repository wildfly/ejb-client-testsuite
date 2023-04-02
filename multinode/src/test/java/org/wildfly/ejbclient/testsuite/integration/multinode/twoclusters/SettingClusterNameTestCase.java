package org.wildfly.ejbclient.testsuite.integration.multinode.twoclusters;

import java.io.IOException;
import java.util.Random;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ContainerHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Headers;
import org.wildfly.extras.creaper.core.online.operations.Operations;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.CLUSTER1_NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.JGROUPS_EE_CHANNEL_RESOURCE;

@SuppressWarnings({"ArquillianDeploymentAbsent"})
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("unignore this manually to run the reproducer, we don't want this to run by default because it hangs the TS")
public class SettingClusterNameTestCase {

	private static OnlineManagementClient creaper_cluster1node1;
	private static ModelNodeResult oldValueResult;

	@ArquillianResource
	private ContainerController containerController;

	@BeforeClass
	public static void prepare() throws IOException {
		creaper_cluster1node1 = ManagementHelpers.createCreaper(CLUSTER1_NODE1.bindAddress, CLUSTER1_NODE1.managementPort);
	}

	@AfterClass
	public static void afterClass() {
		ManagementHelpers.safeClose(creaper_cluster1node1);
	}

	@Test
	public void reproducer() throws Exception {
		containerController.start(CLUSTER1_NODE1.nodeName);
		oldValueResult = new Operations(creaper_cluster1node1).readResource(JGROUPS_EE_CHANNEL_RESOURCE);
		for (int i2 = 0; i2 < 500000; i2++) {
			final String clusterName = "cluster" + new Random().nextInt();
			new Operations(creaper_cluster1node1)
					.headers(Headers.allowResourceServiceRestart())
					.writeAttribute(JGROUPS_EE_CHANNEL_RESOURCE, "cluster", clusterName);
			System.out.println(i2);
		}
		containerController.stop(CLUSTER1_NODE1.nodeName);
	}


	@After
	public void cleanup() throws Exception {
		containerController.start(CLUSTER1_NODE1.nodeName);
		new Operations(creaper_cluster1node1)
				.writeAttribute(JGROUPS_EE_CHANNEL_RESOURCE, "cluster", oldValueResult.get("cluster").asStringOrNull());
		ContainerHelpers.stopContainers(containerController, CLUSTER1_NODE1.nodeName);
	}

}