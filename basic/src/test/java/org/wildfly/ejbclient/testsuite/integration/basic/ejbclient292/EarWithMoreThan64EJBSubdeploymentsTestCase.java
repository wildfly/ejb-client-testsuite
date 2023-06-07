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

package org.wildfly.ejbclient.testsuite.integration.basic.ejbclient292;

import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateful;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateless;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.management.ManagementOperations;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Deploy an EAR which contains more than 64 EJB subdeployments.
 * Check that EJB client is able to perform invocations on EJBs in all the subdeployments.
 *
 * https://issues.redhat.com/browse/EJBCLIENT-292
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarWithMoreThan64EJBSubdeploymentsTestCase {

	public static final Integer SUBDEPLOYMENTS_COUNT = 80;

	protected InitialContextDirectory ctx;

	@Deployment(testable = false)
	public static EnterpriseArchive deployment() {
		final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "big-ear.ear");
		for (int i = 0; i < SUBDEPLOYMENTS_COUNT; i++) {
			final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "subdeployment-" + i + ".jar");
			jar.addPackage(EchoBeanRemote.class.getPackage());
			ear.addAsModule(jar);
		}
		return ear;
	}

	@Before
	public void before() throws NamingException {
		ctx = new InitialContextDirectory.Supplier().get();
	}

	@After
	public void after() {
		ctx.close();
	}

	private static Logger logger = Logger.getLogger(EarWithMoreThan64EJBSubdeploymentsTestCase.class.getName());

	@Test(timeout = 180000)
	public void test() throws Exception {
		/* reload is necessary to trigger EJBCLIENT-292 because it will force the server
		   to send a complete deployment list to the client. Otherwise, if any other tests were
		   run before this one, client would only get incremental updates over time from the server,
		   and the client would never get a message about 64+ deployments being present, so EJBCLIENT-292
		   would not be triggered unless this is the first (or only) test case being run from the test suite.
		 */
		ManagementOperations.reloadServer(60000L); // don't remove!
		// FIXME we should find a way to determine when the server is completely started - with this number of deployments
		// it sometimes happens that we detect that the server is ready (we can already execute
		// management operations), but the deployments are not deployed yet, for now just give the server some additional time
		logger.info("Giving the server some time to finish all deployments before starting invocations...");
		TimeUnit.SECONDS.sleep(30);
		for(int i = 0; i < SUBDEPLOYMENTS_COUNT; i++) {
			logger.info("Looking up stateful EJB in subdeployment number " + i);
			final EchoBeanRemote beanStateful = ctx
					.lookup("big-ear", "subdeployment-" + i, EchoBeanStateful.class, EchoBeanRemote.class, BeanType.STATEFUL, null);
			logger.info("Calling stateful EJB in subdeployment number " + i);
			Assert.assertEquals(8, beanStateful.echo(8));

			logger.info("Looking up stateless EJB in subdeployment number " + i);
			final EchoBeanRemote beanStateless = ctx
					.lookup("big-ear", "subdeployment-" + i, EchoBeanStateless.class, EchoBeanRemote.class, BeanType.STATELESS, null);
			logger.info("Calling stateless EJB in subdeployment number " + i);
			Assert.assertEquals(8, beanStateless.echo(8));
		}
	}
}
