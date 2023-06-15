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

package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The client passes invocation context data to the server using a client-side interceptor.
 * The server side bean is supposed to read the context data and return it back to the client.
 * WeirdCompoundValue is used as the class for the passed context data.
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorAddedProgrammaticallyAsGlobalTestCase {

    public static final String MODULE_NAME = "client-interceptor";

    private InitialContextDirectory ctxDirectory;

    @Deployment(name = MODULE_NAME, testable = false)
    public static JavaArchive deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(ClientInterceptorAddedProgrammaticallyAsGlobalTestCase.class.getPackage());
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        // only for non-scoped EJB client contexts
        Assume.assumeTrue(TestEnvironment.getContextType() == EJBClientContextType.GLOBAL);
    }

    @Before
    public void prepare() {
        ctxDirectory = new InitialContextDirectory.Supplier().get();
        TestEJBClientInterceptor.reset();
    }

    @After
    public void cleanup() {
        TestEJBClientInterceptor.reset();
        ctxDirectory.close();
    }


    @Test
    @SuppressWarnings("unchecked")
    public void doStateful() throws Exception {
        Object o = ctxDirectory.lookupStateful(MODULE_NAME, BeanAcceptingContextDataStateful.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        instance.returnContextData("key");
        Assert.assertTrue(TestEJBClientInterceptor.CALLED_FOR_INVOCATION.get());
        Assert.assertTrue(TestEJBClientInterceptor.CALLED_FOR_INVOCATION_RESULT.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doStateless() throws Exception {
        Object o = ctxDirectory.lookupStateless(MODULE_NAME, BeanAcceptingContextDataStateless.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        instance.returnContextData("key");
        Assert.assertTrue(TestEJBClientInterceptor.CALLED_FOR_INVOCATION.get());
        Assert.assertTrue(TestEJBClientInterceptor.CALLED_FOR_INVOCATION_RESULT.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doSingleton() throws Exception {
        Object o = ctxDirectory.lookupStateless(MODULE_NAME, BeanAcceptingContextDataSingleton.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        instance.returnContextData("key");
        Assert.assertTrue(TestEJBClientInterceptor.CALLED_FOR_INVOCATION.get());
        Assert.assertTrue(TestEJBClientInterceptor.CALLED_FOR_INVOCATION_RESULT.get());
    }

}
