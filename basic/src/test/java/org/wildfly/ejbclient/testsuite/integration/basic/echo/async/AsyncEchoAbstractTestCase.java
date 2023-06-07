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

package org.wildfly.ejbclient.testsuite.integration.basic.echo.async;

import java.util.concurrent.ExecutionException;
import javax.naming.NamingException;

import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.async.echobean.EchoBeanAsyncRemote;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public abstract class AsyncEchoAbstractTestCase {

    public static final String MODULE_NAME = "async-echo-test";

    protected InitialContextDirectory ctx;

    private EchoBeanAsyncRemote bean;

    @Before
    public void before() throws NamingException {
        ctx = new InitialContextDirectory.Supplier().get();
        bean = ctx.lookup(null, MODULE_NAME, getBeanClass(), EchoBeanAsyncRemote.class, getBeanType(), null);
    }

    @After
    public void after() {
        ctx.close();
    }

    protected abstract Class<? extends EchoBeanAsyncRemote> getBeanClass();

    protected abstract BeanType getBeanType();


    @Test
    public void testBooleanBoxed() throws ExecutionException, InterruptedException {
        Assert.assertTrue(bean.echo(Boolean.TRUE).get());
    }


    @Test
    public void testCharacterBoxed() throws ExecutionException, InterruptedException {
        Assert.assertEquals((Character)'x', bean.echo(new Character('x')).get());
    }


    @Test
    public void testDoubleBoxed() throws ExecutionException, InterruptedException {
        Assert.assertEquals(3.0d, bean.echo(new Double(3.0d)).get(), 0);
    }


    @Test
    public void testFloatBoxed() throws ExecutionException, InterruptedException {
        Assert.assertEquals(3.0f, bean.echo(new Float(3.0f)).get(), 0);
    }


    @Test
    public void testIntegerBoxed() throws ExecutionException, InterruptedException {
        Assert.assertEquals((Integer)3, bean.echo(new Integer(3)).get());
    }


    @Test
    public void testLongBoxed() throws ExecutionException, InterruptedException {
        Assert.assertEquals((Long)3L, bean.echo(new Long(3L)).get());
    }


    @Test
    public void testShortBoxed() throws ExecutionException, InterruptedException {
        Assert.assertEquals(new Short((short)3), bean.echo(new Short((short)3)).get());  // lol
    }

}
