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

package org.wildfly.ejbclient.testsuite.integration.basic.contextreferenceholding;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test invoking beans whose InitialContext is no longer referenced and is possibly GC'd already.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ContextReferenceDiscardingTestCase {

    public static final int CALLS = 30;

    private static Logger logger = Logger.getLogger(ContextReferenceDiscardingTestCase.class.getName());

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "echo-contextref.war");
        archive.addClasses(ContextReferenceDiscardingTestCase.class, StatefulCounter.class, StatefulCounterBean.class);
        return archive;
    }

    @BeforeClass
    public static void doesNotWorkWithRemoteNaming() {
        Assume.assumeTrue("Not supported for remote-naming currently",
              !TestEnvironment.getContextType().equals(EJBClientContextType.WILDFLY_NAMING_CLIENT));
    }

    @Test
    public void testInvocationsOnStatefulBeanWhoseContextIsNoLongerReferenced()
            throws NamingException, InterruptedException {

        // FIXME
        Assert.assertFalse("Tests for stateful beans currently skipped because they cause hangs",
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTP));

        final InitialContextDirectory directory = new InitialContextDirectory.Supplier().get();
        final StatefulCounter bean = directory
                .lookupStateful("echo-contextref", StatefulCounterBean.class, StatefulCounter.class);

        WeakReference<InitialContext> weak = new WeakReference<>(directory.getInitialContext());
        directory.discardReferenceToInitialContext();

        // wait until the InitialContext is discarded by garbage collector
        // then try one more invocation on the EJB
        for(int i = 0; i < CALLS; i++) {
            System.gc();
            TimeUnit.SECONDS.sleep(1);
            if(weak.get() == null) {
                logger.info("Initial ctx reference was garbage collected.. ");
                Assert.assertTrue(bean.incrementAndGet() == i);
                break;
            }
            logger.info("Call " + i + "/" + CALLS);
        }

    }




}
