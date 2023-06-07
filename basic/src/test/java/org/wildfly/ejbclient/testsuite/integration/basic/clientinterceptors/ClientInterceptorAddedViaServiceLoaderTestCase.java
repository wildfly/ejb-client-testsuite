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

import javax.naming.NamingException;

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
import org.junit.Ignore;

import static org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors.TestEJBClientInterceptorAddingContextDataForAddingByServiceLoader.DEFAULT_KEY;
import static org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors.TestEJBClientInterceptorAddingContextDataForAddingByServiceLoader.DEFAULT_VALUE;

/**
 * @author Jan Martiska
 */
@Ignore
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorAddedViaServiceLoaderTestCase {

    public static final String MODULE_NAME = "client-interceptor-service-loader";

    private InitialContextDirectory ctxDirectory;

    @Deployment(name = MODULE_NAME, testable = false)
    public static JavaArchive deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(ClientInterceptorAddedViaServiceLoaderTestCase.class.getPackage());
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        // remote-naming = doesn't work at all
        // global = works
        Assume.assumeTrue(TestEnvironment.getContextType() != EJBClientContextType.WILDFLY_NAMING_CLIENT);
    }


    @Before
    public void prepare() {
        ctxDirectory = new InitialContextDirectory.Supplier().get();
    }

    @After
    public void cleanup() {
        ctxDirectory.close();
    }


    @Test
    @SuppressWarnings("unchecked")
    public void doStateful() throws NamingException {
        Object o = ctxDirectory.lookupStateful(MODULE_NAME, BeanAcceptingContextDataStateful.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        WeirdCompoundValue returned = instance.returnContextData(DEFAULT_KEY);
        Assert.assertNotNull("Server did not return context data", returned);
        Assert.assertEquals("Server returned context data, but it is corrupt", DEFAULT_VALUE, returned);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doStateless() throws NamingException {
        Object o = ctxDirectory.lookupStateless(MODULE_NAME, BeanAcceptingContextDataStateless.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        WeirdCompoundValue returned = instance.returnContextData(DEFAULT_KEY);
        Assert.assertNotNull("Server did not return context data", returned);
        Assert.assertEquals("Server returned context data, but it is corrupt", DEFAULT_VALUE, returned);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doSingleton() throws NamingException {
        Object o = ctxDirectory.lookupStateless(MODULE_NAME, BeanAcceptingContextDataSingleton.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        WeirdCompoundValue returned = instance.returnContextData(DEFAULT_KEY);
        Assert.assertNotNull("Server did not return context data", returned);
        Assert.assertEquals("Server returned context data, but it is corrupt", DEFAULT_VALUE, returned);
    }

}
