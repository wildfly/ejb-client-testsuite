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

package org.wildfly.ejbclient.testsuite.integration.basic.echo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateful;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Get a stateful EJB proxy, pass it to multiple threads and use it concurrently on the client side.
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ConcurrentStatefulProxyUsageTestCase {

    public static final int NUMBER_INVOCATIONS = 5000;

    protected InitialContextDirectory ctx;

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "basic-echo-test.war");
        archive.addPackage(EchoBeanRemote.class.getPackage());
        return archive;
    }

    @Before
    public void before() throws NamingException {
        ctx = new InitialContextDirectory.Supplier().get();
    }

    @After
    public void after() {
        ctx.close();
    }

    @Test
    public void test() throws Exception {
        final EchoBeanRemote bean = ctx
                .lookup(null, "basic-echo-test", EchoBeanStateful.class, EchoBeanRemote.class,
                        BeanType.STATEFUL, null);
        final ExecutorService threadPool = Executors.newFixedThreadPool(30);
        try {
            @SuppressWarnings("unchecked") final CompletableFuture<Void>[] futures
                    = new CompletableFuture[NUMBER_INVOCATIONS];
            for (int i = 0; i < NUMBER_INVOCATIONS; i++) {
                futures[i] = CompletableFuture.runAsync(
                        () -> { bean.echo("blabla");
                            System.out.println("Done"); },
                        threadPool);
            }
            try {
                CompletableFuture.allOf(futures).get(180, TimeUnit.SECONDS);
            } catch(Exception e) {
                System.out.println("Failing the test, something failed..");
                e.printStackTrace();
                throw e;
            }
        } finally {
            threadPool.shutdown();
        }
    }
}
