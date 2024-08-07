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
