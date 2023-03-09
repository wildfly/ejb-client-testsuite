/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.ejbclient.testsuite.integration.basic.distinctname;

import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that invocation on EJBs deployed in a deployment with distinct name works successfully
 *
 * @author Jaikiran Pai // deliberately stolen verbatim from AS7 testsuite by jmartisk
 */
public abstract class DistinctNameTestCase {

    protected static InitialContextDirectory ctxDirectory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // distinct names are not supported for invocations over remote-naming
        Assume.assumeFalse(System.getProperty("context.type").equalsIgnoreCase("wildfly-naming-client"));

        ctxDirectory = new InitialContextDirectory.Supplier().get();
    }

    /**
     * Test that invocation on a stateless bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSLSBInvocation() throws Exception {
        // EAR or JAR?
        final Echo bean;
        
        if(!getAppName().isEmpty()){//EAR
            bean = ctxDirectory.lookupStatelessWithDN(getAppName(), getModuleName(), StatelessEcho.class, Echo.class, getDistinctName());
        }else{//JAR, WAR
            bean = ctxDirectory.lookupStatelessWithDN(getModuleName(), StatelessEcho.class, Echo.class, getDistinctName());          
        }
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote stateless bean", msg, echo);        
    }

    /**
     * Test that invocation on a stateful bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSFSBInvocation() throws Exception {

            // FIXME
            Assert.assertFalse("Tests for stateful beans currently skipped because they cause hangs",
                    TestEnvironment.getConnectorType().equals(ConnectorType.HTTP));

        final Echo bean;
        if(!getAppName().isEmpty()){ //EAR
            bean = ctxDirectory.lookupStatefulWithDN(getAppName(), getModuleName(), StatefulEcho.class, Echo.class, getDistinctName());
        } else{ //JAR, WAR
            bean = ctxDirectory.lookupStatefulWithDN(getModuleName(), StatefulEcho.class, Echo.class, getDistinctName());
        }
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote stateful bean", msg, echo);
    }

    /**
     * Test that invocation on a singleton bean, deployed in a deployment with distinct-name, works fine
     *
     * @throws Exception
     */
    @Test
    public void testRemoteSingletonInvocation() throws Exception {
        final Echo bean;
        if(!getAppName().isEmpty()){ //EAR
            bean = ctxDirectory.lookupSingletonWithDN(getAppName(), getModuleName(), SingletonEcho.class, Echo.class, getDistinctName());
        } else { //JAR, WAR
            bean = ctxDirectory.lookupSingletonWithDN(getModuleName(), SingletonEcho.class, Echo.class, getDistinctName());
        }
        Assert.assertNotNull("Lookup returned a null bean proxy", bean);
        final String msg = "Hello world from a really remote client!!!";
        final String echo = bean.echo(msg);
        Assert.assertEquals("Unexpected echo returned from remote singleton bean", msg, echo);
    }

    protected abstract String getAppName();

    protected abstract String getModuleName();

    protected abstract String getDistinctName();
}
