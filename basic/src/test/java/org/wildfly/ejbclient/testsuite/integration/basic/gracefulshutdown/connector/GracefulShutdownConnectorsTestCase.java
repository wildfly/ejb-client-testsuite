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

package org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.connector;

import java.io.IOException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateless;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.GracefulShutdownHelper.OPERATION_RESUME;
import static org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.GracefulShutdownHelper.OPERATION_SUSPEND;

/**
 * Test that after graceful shutdown, the connectors really stop accepting requests.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class GracefulShutdownConnectorsTestCase {

    public static final String MODULE_NAME = "bare-remoting-shutdown";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        archive.addPackage(EchoBeanRemote.class.getPackage());
        return archive;
    }


    @ContainerResource
    private ManagementClient mc;

    protected InitialContextDirectory ctx;

    @Test
    public void testGracefulShutdown() throws NamingException, IOException {
        ctx = new InitialContextDirectory.Supplier().get();
        final EchoBeanRemote bean = ctx
                .lookupStateless(MODULE_NAME, EchoBeanStateless.class, EchoBeanRemote.class);

        // test that it works normally before suspending
        bean.echo("Hi");
        System.out.println("**************************************** activating graceful shutdown");
        final ModelControllerClient mcc = mc.getControllerClient();
        // perform the suspend
        final ModelNode suspendResult = mcc.execute(OPERATION_SUSPEND);
        System.out.println("suspend result: " + suspendResult.toJSONString(false));


        try {
            // now the EJB call should fail
            System.out.println("Trying to invoke EJB...");
            bean.echo("Hi");
            Assert.fail("The call shouldn't have gone through after suspending the server");
        } catch (Exception ex) {
            System.out.println("OK, caught expected exception:");
            ex.printStackTrace();
        } finally {

            // resume back
            System.out.println("*************************** resuming server operations");
            final ModelNode resumeResult = mcc.execute(OPERATION_RESUME);
            System.out.println("Result of resume operation: " + resumeResult.toJSONString(false));

        }


        ctx.close();


    }


}
