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

import javax.naming.NamingException;

import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jan Martiska
 */
public abstract class BasicEchoAbstractTestCase {

    protected InitialContextDirectory ctx;

    private EchoBeanRemote bean;

    @Before
    public void before() throws NamingException {
        ctx = new InitialContextDirectory.Supplier().get();
        bean = ctx.lookup(null, "basic-echo-test", getBeanClass(), EchoBeanRemote.class, getBeanType(), null);
    }

    @After
    public void after() {
        ctx.close();
    }

    protected abstract Class<? extends EchoBeanRemote> getBeanClass();

    protected abstract BeanType getBeanType();

    @Test
    public void testBooleanPrimitive() {
        Assert.assertTrue(bean.echo(true));
    }

    @Test
    public void testBooleanBoxed() {
        Assert.assertTrue(bean.echo(Boolean.TRUE));
    }

    @Test
    public void testCharacterPrimitive() {
        Assert.assertEquals('x', bean.echo('x'));
    }

    @Test
    public void testCharacterBoxed() {
        Assert.assertEquals((Character)'x', bean.echo(new Character('x')));
    }

    @Test
    public void testDoublePrimitive() {
        Assert.assertEquals(3.0d, bean.echo(3.0d), 0);
    }

    @Test
    public void testDoubleBoxed() {
        Assert.assertEquals(3.0d, bean.echo(new Double(3.0d)), 0);
    }

    @Test
    public void testFloatPrimitive() {
        Assert.assertEquals(3.0f, bean.echo(3.0f), 0);
    }

    @Test
    public void testFloatBoxed() {
        Assert.assertEquals(3.0f, bean.echo(new Float(3.0f)), 0);
    }

    @Test
    public void testIntegerPrimitive() {
        Assert.assertEquals(3, bean.echo(3));
    }

    @Test
    public void testIntegerBoxed() {
        Assert.assertEquals((Integer)3, bean.echo(new Integer(3)));
    }

    @Test
    public void testLongPrimitive() {
        Assert.assertEquals(3L, bean.echo(3L));
    }

    @Test
    public void testLongBoxed() {
        Assert.assertEquals((Long)3L, bean.echo(new Long(3L)));
    }

    @Test
    public void testShortPrimitive() {
        Assert.assertEquals((short)3, bean.echo((short)3));
    }

    @Test
    public void testShortBoxed() {
        Assert.assertEquals(new Short((short)3), bean.echo(new Short((short)3)));  // lol
    }

    /**
     * \uD83C\uDF79 is a code point which takes up two java Characters.
     * Check that server doesn't mess up Strings with multi-character code points.
     */
    @Test
    public void testStringSpecial(){
        final String returned = bean.echo("\uD83C\uDF79");
        Assert.assertTrue("The returned string should consist of one code point", returned.codePoints().count() == 1);
        Assert.assertTrue("The returned string should have a length of two", returned.length() == 2);
        Assert.assertEquals("\uD83C\uDF79", returned);
    }
}
