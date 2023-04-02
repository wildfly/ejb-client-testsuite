package org.wildfly.ejbclient.testsuite.integration.multinode.security;

import java.security.Security;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whoami.WhoAmIRemote;
import org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whoami.WhoAmIStateless;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers.isIPv6;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.SecurityHelpers.createTestingUsers;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.SecurityHelpers.removeTestingUsers;

/**
 * see WFLY-9357
 */

@SuppressWarnings("ArquillianDeploymentAbsent")
@RunWith(Arquillian.class)
@RunAsClient
public class OverrideSecuritySettingsInWildflyConfigTestCase {

	public static JavaArchive createDeployment() {
		final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "security.jar");
		jar.addClasses(WhoAmIRemote.class, WhoAmIStateless.class);
		return jar;
	}

	private final JavaArchive DEPLOYMENT = createDeployment();

	@ArquillianResource
	private ContainerController containerController;

	OnlineManagementClient creaper;

	@Before
	public void before() throws Exception {
		setLoggerPrefix("NODE1", NODE1.homeDirectory, NODE1.configurationXmlFile);
		createTestingUsers(NODE1.homeDirectory);
		containerController.start(NODE1.nodeName);
		creaper = createCreaper(NODE1.bindAddress, NODE1.managementPort);
		DeploymentHelpers.deploy(DEPLOYMENT, creaper);
	}

	@After
	public void cleanup() throws Exception {
		DeploymentHelpers.undeploy(DEPLOYMENT.getName(), creaper);
		containerController.stop(NODE1.nodeName);
		setLoggerPrefix("", NODE1.homeDirectory, NODE1.configurationXmlFile);
		removeTestingUsers(NODE1.homeDirectory);
	}

	/**
	 * There is an authentication context with a username/password configured.
	 * Add SECURITY_PRINCIPAL and SECURITY_CREDENTIALS properties when creating InitialContext.
	 * These properties should override the credentials from the authentication context.
	 *
	 * TODO similar test which uses wildfly-config.xml instead of programmatic ctx
	 */
	@Test
	public void invokeUsingAuthCtxAndThenUsingProperty() throws NamingException {
		Security.addProvider(new WildFlyElytronProvider());
		setWildflyConfigXml("secured");

		AuthenticationConfiguration userConf = AuthenticationConfiguration.empty()
				.useDefaultProviders()
				.setSaslMechanismSelector(SaslMechanismSelector.fromString("DIGEST-MD5"))
				.useName("joe")
				.usePassword("joeIsAwesome2013!");
		AuthenticationContext ctx = AuthenticationContext.empty().with(MatchRule.ALL, userConf);
		AuthenticationContext.getContextManager().setGlobalDefault(ctx);

		// without explicit credentials, AuthenticationContext should be used
		{
			final Properties properties_justFactory = new Properties();
			properties_justFactory.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
			final InitialContext ejbCtx = new InitialContext(properties_justFactory);
			try {
				final WhoAmIRemote bean = (WhoAmIRemote) ejbCtx
						.lookup("ejb:/security/" + WhoAmIStateless.class.getSimpleName() + "!"
								+ WhoAmIRemote.class.getName());
				Assert.assertEquals("joe", bean.whoAmI());
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}

		// now with explicit credentials
		{
			final Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
			properties.put(Context.SECURITY_PRINCIPAL, "joe2");
			properties.put(Context.SECURITY_CREDENTIALS, "joeIsAwesome2014!");
			final InitialContext ejbCtx = new InitialContext(properties);
			try {
				final WhoAmIRemote bean = (WhoAmIRemote) ejbCtx
						.lookup("ejb:/security/" + WhoAmIStateless.class.getSimpleName() + "!"
								+ WhoAmIRemote.class.getName());
				Assert.assertEquals("joe2", bean.whoAmI());
			} finally {
				MiscHelpers.safeCloseEjbClientContext(ejbCtx);
			}
		}
	}


	public void setWildflyConfigXml(String name) {
		if ( !isIPv6() ) {
			System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
					"org/wildfly/ejbclient/testsuite/integration/multinode/security/" + name + ".xml").toString());
		} else {
			System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
					"org/wildfly/ejbclient/testsuite/integration/multinode/security/" + name + "-ipv6.xml").toString());
		}
	}


}
