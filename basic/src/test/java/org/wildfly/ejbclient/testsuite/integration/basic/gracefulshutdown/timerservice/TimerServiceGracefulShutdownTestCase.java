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

package org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.timerservice;

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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.GracefulShutdownHelper.OPERATION_RESUME;
import static org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.GracefulShutdownHelper.OPERATION_SUSPEND;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TimerServiceGracefulShutdownTestCase {

    public static final String MODULE_NAME = "timer-service-graceful-shutdown";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        archive.addPackage(TimedCountingBean.class.getPackage());
        return archive;
    }

    @ContainerResource
    private ManagementClient mc;

    protected InitialContextDirectory ctx;

    @Test
    public void testThatTimersDontGoOffAfterSuspend() throws NamingException, IOException, InterruptedException {
        ctx = new InitialContextDirectory.Supplier().get();
        final TimedCountingBeanRemote bean = ctx
                .lookupSingleton(MODULE_NAME,
                        TimedCountingBean.class,
                        TimedCountingBeanRemote.class);

        // test that it works normally before suspending
        Thread.sleep(5000);
        Assert.assertTrue("The timer should work now! There should be some ticks done.", bean.getTicks() > 0);


        System.out.println("**************************************** activating graceful shutdown");
        final ModelControllerClient mcc = mc.getControllerClient();

        // get the number of ticks before suspend..
        int ticksBefore = bean.getTicks();
        // perform the suspend
        final ModelNode suspendResult = mcc.execute(OPERATION_SUSPEND);
        System.out.println("suspend result: " + suspendResult.toJSONString(false));

        try {
            // now wait some time....... during that time, no more timeouts should occur
            System.out.println("*************************** waiting 20 seconds before resuming");
            Thread.sleep(20000);
        } finally {
            // resume back
            System.out.println("*************************** resuming server operations");
            final ModelNode resumeResult = mcc.execute(OPERATION_RESUME);
            System.out.println("Result of resume operation: " + resumeResult.toJSONString(false));
        }

        // wait until the :resume completes
        // ........and check that the timer didn't go off during suspend
        int ticksAfter = 0;
        int attempts = 0;
        System.out.println("********************* attempting to call the singleton bean again.....");
        Throwable beanCallingError = null;
        while(attempts < 100) {
            try {
                ticksAfter = bean.getTicks();
                break;
            } catch(Exception x) {
                attempts++;
                Thread.sleep(100);
                beanCallingError = x;
            }
        }
        if(attempts >= 100) {
            throw new RuntimeException("Singleton bean was not accessible within reasonable timeout after resuming", beanCallingError);
        }

        Assert.assertTrue("The tick count should around be the same, but ticksAfterSuspendResume=" + ticksAfter
                + " and ticksBeforeSuspend=" + ticksBefore, ticksAfter - ticksBefore < 6);

        // check that the timer really resumed...
        Thread.sleep(5000);
        int ticksAfterAfter = bean.getTicks();
        Assert.assertTrue("The tick count should around be the same", ticksAfterAfter - ticksAfter > 1);

        ctx.close();
    }
}
