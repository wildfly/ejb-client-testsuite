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

package org.wildfly.ejbclient.testsuite.integration.basic.security;

import java.util.concurrent.ExecutionException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
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
public class WhoAmITestCase {

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
                    .lookupStateless(ARCHIVE_NAME, WhoAmIBean.class, WhoAmIRemote.class);
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
                    .lookupStateless(ARCHIVE_NAME, WhoAmIBean.class, WhoAmIRemote.class);
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
                    .lookupStateless(ARCHIVE_NAME, WhoAmIBean.class, WhoAmIRemote.class);
            Assert.assertTrue("User should be in role 'users'", whoami.amIInRole("users"));
        }
    }

    @Test
    public void isCallerInRole2() throws Exception {
        Assume.assumeTrue(TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.USER));
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .lookupStateless(ARCHIVE_NAME, WhoAmIBean.class, WhoAmIRemote.class);
            Assert.assertFalse("amIInRole should return false for roles which don't exist", whoami.amIInRole("Dunstabzugshaube"));
        }
    }

    /**
     * Regression tests for WEJBHTTP-119
     *
     * Similar like isCallerInRole2, EJB prefix is used in case of WILDFLY_NAMING_CLIENT
     */
    @Test
    public void isCallerInRoleNameWithEjbPrefix() throws Exception {
        Assume.assumeTrue(TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.USER));
        Assume.assumeTrue(TestEnvironment.getContextType() == EJBClientContextType.WILDFLY_NAMING_CLIENT); // other context types uses ejb prefix by default
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .getBean("ejb:/whoami-test/" + WhoAmIBean.class.getSimpleName() + "!" + WhoAmIRemote.class.getCanonicalName(), WhoAmIRemote.class);
            Assert.assertFalse("amIInRole should return false for roles which don't exist", whoami.amIInRole("Dunstabzugshaube"));
        }
    }

    /**
     * Regression tests for WEJBHTTP-119
     *
     * Similar like isCallerInRole2, EJB prefix is not used in case of WILDFLY_NAMING_CLIENT
     */
    @Test
    public void isCallerInRoleNameWithoutEjbPrefix() throws Exception {
        Assume.assumeTrue(TestEnvironment.getAuthenticationType().equals(ServerAuthenticationType.USER));
        Assume.assumeTrue(TestEnvironment.getContextType() == EJBClientContextType.WILDFLY_NAMING_CLIENT); // other context types uses ejb prefix by default
        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .getBean("whoami-test/" + WhoAmIBean.class.getSimpleName() + "!" + WhoAmIRemote.class.getCanonicalName(), WhoAmIRemote.class);
            Assert.assertFalse("amIInRole should return false for roles which don't exist", whoami.amIInRole("Dunstabzugshaube"));
        }
    }
}
