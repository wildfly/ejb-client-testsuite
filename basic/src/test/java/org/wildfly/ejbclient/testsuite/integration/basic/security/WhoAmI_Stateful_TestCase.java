package org.wildfly.ejbclient.testsuite.integration.basic.security;

import java.util.concurrent.ExecutionException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ServerAuthenticationType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WhoAmI_Stateful_TestCase {

    public static final String ARCHIVE_NAME = "whoami-test";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        archive.addPackage(WhoAmIBean.class.getPackage());
        return archive;
    }

    @Test
    public void testLoggedInUser() throws NamingException {
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .lookupStateful(ARCHIVE_NAME, WhoAmIBeanStateful.class, WhoAmIRemote.class);
            final String username = whoami.whoAmI();
            String expectedName = TestEnvironment.USERNAME;
            if (TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.LOCAL)) {
                expectedName = "$local";
            }
            Assert.assertEquals("Unexpected username of logged in user", expectedName, username);
        }
    }

    /**
     * EJB 3.2 4.5.4 Security
     * The caller security principal propagates with an asynchronous method invocation. Caller security prin-
     * cipal propagation behaves exactly the same for asynchronous method invocations as it does for synchro-
     * nous session bean invocations.
     */
    @Test
    public void testSecurityContextPropagationInAsynchronousInvocation()
            throws NamingException, ExecutionException, InterruptedException {
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .lookupStateful(ARCHIVE_NAME, WhoAmIBeanStateful.class, WhoAmIRemote.class);
            final String username = whoami.whoAmIAsync().get();
            String expectedName = TestEnvironment.USERNAME;
            if (TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.LOCAL)) {
                expectedName = "$local";
            }
            Assert.assertEquals("Unexpected username of logged in user", expectedName, username);
        }
    }

    @Test
    public void isCallerInRole() throws Exception {
        Assume.assumeTrue(TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.USER));
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .lookupStateful(ARCHIVE_NAME, WhoAmIBeanStateful.class, WhoAmIRemote.class);
            Assert.assertTrue(whoami.amIInRole("users"));
        }
    }

    @Test
    public void isCallerInRole2() throws Exception {
        Assume.assumeTrue(TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.USER));
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .lookupStateful(ARCHIVE_NAME, WhoAmIBeanStateful.class, WhoAmIRemote.class);
            Assert.assertFalse(whoami.amIInRole("Dunstabzugshaube"));
        }
    }
}
