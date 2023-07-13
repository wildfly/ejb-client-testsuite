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

package org.wildfly.ejbclient.testsuite.integration.basic.security.identityswitching;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.security.WhoAmIBeanStateful;
import org.wildfly.ejbclient.testsuite.integration.basic.security.WhoAmIRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ServerAuthenticationType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * Test identity multiplexing over one remoting connection.
 * Lookup a bean, invoke it in one user's authentication context
 * and then invoke it using another user's authentication context.
 * We should be able to switch our identity for each invocation separately.
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IdentitySwitchingTestCase {

    public static final String ARCHIVE_NAME = "identity-switching";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        archive.addPackage(IdentitySwitchingTestCase.class.getPackage());
        archive.addClasses(WhoAmIRemote.class, WhoAmIBeanStateful.class);
        return archive;
    }

    @BeforeClass
    public static void prerequisities() {
        Assume.assumeTrue(TestEnvironment.getAuthenticationType() == ServerAuthenticationType.USER);
        Assume.assumeTrue(TestEnvironment.getContextType() == EJBClientContextType.GLOBAL);
    }


    @Test
    public void testInvokeStatefulBeanWithMultipleIdentities() throws NamingException {
        AuthenticationConfiguration joe2configuration = AuthenticationConfiguration.EMPTY
                .useDefaultProviders()
                .setSaslMechanismSelector(SaslMechanismSelector.fromString("DIGEST-MD5"))
                .useRealm("ApplicationRealm")
                .useName("joe2")
                .usePassword("blablabla!");

        try (final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final WhoAmIRemote whoami = ctx
                    .lookupStateful(ARCHIVE_NAME, WhoAmIBeanStateful.class, WhoAmIRemote.class);

            final AuthenticationContext joeContext = AuthenticationContext.getContextManager()
                    .getGlobalDefault();
            final AuthenticationContext joe2Context = AuthenticationContext.empty()
                    .with(MatchRule.ALL, joe2configuration);

            joeContext.run(() ->
                    Assert.assertEquals("joe", whoami.whoAmI())
            );
            joe2Context.run(() ->
                    Assert.assertEquals("joe2", whoami.whoAmI())
            );
            joeContext.run(() ->
                    Assert.assertEquals("joe", whoami.whoAmI())
            );
        }
    }

}
