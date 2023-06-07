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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BeanPoolOfSize1TestCase {

    public static final String DEPLOYMENT_NAME = "bean-pool-management";

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(managed = false, name = DEPLOYMENT_NAME)
    public static WebArchive deployment1() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        archive.addClasses(LongProcessingBeanRemote.class,
                LongProcessingBeanStateless.class);
        return archive;
    }

    private InitialContextDirectory ctx;


    @BeforeClass
    public static void beforeClass() {
        // FIXME
        Assume.assumeFalse("BeanPoolSize1TestCase currently skipped because it is flakey",
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTP) ||
                        TestEnvironment.getConnectorType().equals(ConnectorType.HTTPS));
    }
    @Before
    public void before() throws NamingException {
        ctx = new InitialContextDirectory.Supplier().get();
    }

    /**
     * Set SLSB instance pool size to 1.
     * Run 10 invocations on a SLSB bean (concurrently; each invocation takes 1 second to complete).
     * Expect that all invocations will be routed to the same SLSB instance.
     */
    @Test
    public void slsbWithPoolSize1()
            throws NamingException, IOException, InterruptedException, ExecutionException, TimeoutException {
        BeanPoolManagementTools.createStrictMaxBeanInstancePool(managementClient.getControllerClient(),
                "size1pool", null, 1, null, null);
        deployer.deploy(DEPLOYMENT_NAME);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            final LongProcessingBeanRemote bean = ctx
                    .lookupStateless(DEPLOYMENT_NAME, LongProcessingBeanStateless.class,
                            LongProcessingBeanRemote.class);
            bean.processAndReturnInstanceHashCode(0);
            Supplier<Integer> oneEjbInvocation = () -> {
                System.out.println("Invoking bean from client-side thread: " + Thread.currentThread().getName());
                try {
                    return bean.processAndReturnInstanceHashCode(1);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new RuntimeException(t);
                }
            };
            // run ten invocations and check that all will go to the same SLSB instance
            // differentiate between instances using the hashCode
            final Map<Integer, List<Integer>> hashCodes = IntStream.range(0, 10)
                    .parallel()
                    .mapToObj((i) -> CompletableFuture.supplyAsync(oneEjbInvocation, pool))
                    .map(this::safeGet)
                    .collect(Collectors.groupingBy(i -> i));
            Assert.assertEquals("All invocations should go to just one SLSB instance!", 1L, hashCodes.size());
        } finally {
            pool.shutdown();
            deployer.undeploy(DEPLOYMENT_NAME);
            BeanPoolManagementTools
                    .removeStrictMaxBeanInstancePool(managementClient.getControllerClient(), "size1pool");
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

