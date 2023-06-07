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

package org.wildfly.ejbclient.testsuite.integration.basic.beanpool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BeanPoolWithShortTimeoutTestCase {

    public static final String DEPLOYMENT_NAME = "bean-pool-management";

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(managed = false, name = DEPLOYMENT_NAME)
    public static WebArchive deployment2() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        archive.addClasses(LongProcessingBeanRemote.class,
                LongProcessingBeanStateless2.class);
        return archive;
    }

    private InitialContextDirectory ctx;

    @Before
    public void before() throws NamingException {
        ctx = new InitialContextDirectory.Supplier().get();
    }


    /**
     * Pool configuration: [size = 1, timeout = 500 milliseconds]
     * Run two invocations at once (each invocation takes 2 seconds).
     * Expect that the second invocation attempt will fail due to a timeout.
     */
    @Test
    public void slsbWithPoolSize1AndShortTimeout() throws Exception {
        BeanPoolManagementTools.createStrictMaxBeanInstancePool(managementClient.getControllerClient(),
                "size1poolTimeout", null, 1, 500L, TimeUnit.MILLISECONDS);
        deployer.deploy(DEPLOYMENT_NAME);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            final LongProcessingBeanRemote bean = ctx
                    .lookupStateless(DEPLOYMENT_NAME, LongProcessingBeanStateless2.class,
                            LongProcessingBeanRemote.class);
            bean.processAndReturnInstanceHashCode(0);
            Supplier<Integer> oneEjbInvocation = () -> {
                System.out.println("Invoking bean from client-side thread: " + Thread.currentThread().getName());
                try {
                    return bean.processAndReturnInstanceHashCode(2);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new RuntimeException(t);
                }
            };
            final CompletableFuture<Integer> firstInvocation = CompletableFuture.supplyAsync(oneEjbInvocation, pool);
            Thread.sleep(200);
            final CompletableFuture<Integer> secondInvocation = CompletableFuture.supplyAsync(oneEjbInvocation, pool);
            try {
                secondInvocation.get(5, TimeUnit.SECONDS);
                Assert.fail("Second invocation should time out");
            } catch(ExecutionException e) {
                // this should make sure it was due to timeout
                // better not check it in more detail, the exact format of exceptions from EJB client tends to change quite often
                Assert.assertTrue("Expecting a failure due to timeout, but the message was: " + e.getMessage(),
                        e.getMessage().contains("MILLISECONDS"));
            }
        } finally {
            pool.shutdown();
            deployer.undeploy(DEPLOYMENT_NAME);
            BeanPoolManagementTools
                    .removeStrictMaxBeanInstancePool(managementClient.getControllerClient(), "size1poolTimeout");
        }
    }

    public <T> T safeGet(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

