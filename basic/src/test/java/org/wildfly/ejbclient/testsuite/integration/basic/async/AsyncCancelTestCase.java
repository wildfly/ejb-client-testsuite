package org.wildfly.ejbclient.testsuite.integration.basic.async;

import java.util.concurrent.Future;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AsyncCancelTestCase {

    public static final String ARCHIVE_NAME = "async-cancel";

    private static Logger logger = Logger.getLogger(AsyncCancelTestCase.class.getName());

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        archive.addPackage(LongRunningAsyncBean.class.getPackage());
        return archive;
    }

    /**
     * If a client calls cancel on its Future object, the container will attempt to cancel the associated asyn-
     chronous invocation only if that invocation has not already been dispatched. There is no guarantee that
     an asynchronous invocation can be cancelled, regardless of how quickly cancel is called after the cli-
     ent receives its Future object. If the asynchronous invocation cannot be cancelled, the method must
     return false. If the asynchronous invocation is successfully cancelled, the method must return true.

     The mayInterruptIfRunning flag controls whether, in the case that the asynchronous invocation
     can not be cancelled, the target enterprise bean should have visibility to the client’s cancel attempt. If
     the mayInterruptIfRunning flag is set to true, then subsequent calls to the SessionCon-
     text.wasCancelCalled method from within the associated dispatched asynchronous invocation
     must return true. If the mayInterruptIfRunning flag is set to false, then subsequent calls to
     the SessionContext.wasCancelCalled method from within the associated dispatched asyn-
     chronous invocation must return false.

     Note that all the client Future cancel semantics (isCancelled, CancellationException,
     etc.) depend only on the result of Future.cancel. If the dispatched asynchronous method does
     decide to short circuit its processing as a result of checking SessionContext, it is the responsibility
     of the Bean Provider to decide how to convey that information to the client. Typically, that is done
     through a special return value or exception delivered via Future.get().
     */
    @Test
    public void testCancel() throws Exception {
        // FIXME
        Assume.assumeFalse("AsyncCancelTestCase currently flaky with HTTP and HTTPS.",
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTP) ||
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTPS));
        LongRunningAsyncRemote bean;
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            bean = ctx
                    .lookupStateful(ARCHIVE_NAME, LongRunningAsyncBean.class, LongRunningAsyncRemote.class);
            final Future<Integer> future = bean.computeAnswerToEverything();
            Thread.sleep(3000); // wait so it really gets dispatched and started
            future.cancel(true);
            Thread.sleep(100);
            Assert.assertTrue("The bean should have been notified that cancel was called", bean.isCancelCalled());
        }

    }
}
