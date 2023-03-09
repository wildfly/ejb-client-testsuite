package org.wildfly.ejbclient.testsuite.integration.basic.io;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.io.bean.ThreadNameReturningBean;
import org.wildfly.ejbclient.testsuite.integration.basic.io.bean.ThreadNameReturningBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.management.ManagementOperations;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */

// introduced by WFLY-5886
// it's possible to choose whether EJB invocations should be done in EJB thread pool or the IO subsystem's worker pool
@RunWith(Arquillian.class)
@RunAsClient
public class ExecuteInWorkerTestCase {

    public static final String ARCHIVE_NAME = "execute-in-worker";

    @Deployment(name = ARCHIVE_NAME, testable = false)
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        archive.addPackage(ThreadNameReturningBean.class.getPackage());
        return archive;
    }

    @ArquillianResource
    private ManagementClient managementClient;

    // ejb invocations should by default be done in IO subsystem's task pool
    @Test
    @InSequence(0)
    public void executeInWorkerPool() throws Exception {
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final ThreadNameReturningBeanRemote bean = ctx
                    .lookupStateless(ARCHIVE_NAME, ThreadNameReturningBean.class,
                            ThreadNameReturningBeanRemote.class);
            final String threadName = bean.getExecutorThreadName();
            System.out.println("---- thread name :: " + threadName);
            Assert.assertFalse("Invocation was executed by thread " + threadName,
                    threadName.toLowerCase().contains("ejb"));
        }
    }

    @Test
    @InSequence(1)
    public void setExecuteInThreadPool() throws Exception {
        final ModelNode op = ManagementOperations
                .createSetExecuteInWorkerOperation(false);
        System.out.println(managementClient.getControllerClient().execute(op));
        System.out.println("---------- RELOADING");
        ManagementOperations.reloadServer();
    }

    @Test
    @InSequence(2)
    public void executeInEJBPool() throws Exception {
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final ThreadNameReturningBeanRemote bean = ctx
                    .lookupStateless(ARCHIVE_NAME, ThreadNameReturningBean.class,
                            ThreadNameReturningBeanRemote.class);
            final String threadName = bean.getExecutorThreadName();
            System.out.println("---- thread name :: " + threadName);
            Assert.assertTrue("Invocation was executed by thread " + threadName,
                    threadName.toLowerCase().contains("ejb"));
        }
    }

    @Test
    @InSequence(3)
    public void setExecuteInEJBPool() throws Exception {

        final ModelNode op = ManagementOperations
                .createSetExecuteInWorkerOperation(true);
        System.out.println(managementClient.getControllerClient().execute(op));
        System.out.println("---------- RELOADING");
        ManagementOperations.reloadServer();
    }

    // test the default again to make sure that the configuration change was reverted successfully
    @Test
    @InSequence(4)
    public void executeInWorkerPoolAgain() throws Exception {
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final ThreadNameReturningBeanRemote bean = ctx
                    .lookupStateless(ARCHIVE_NAME, ThreadNameReturningBean.class,
                            ThreadNameReturningBeanRemote.class);
            final String threadName = bean.getExecutorThreadName();
            System.out.println("---- thread name :: " + threadName);
            Assert.assertFalse("Invocation was executed by thread " + threadName,
                    threadName.toLowerCase().contains("ejb"));
        }
    }

}
