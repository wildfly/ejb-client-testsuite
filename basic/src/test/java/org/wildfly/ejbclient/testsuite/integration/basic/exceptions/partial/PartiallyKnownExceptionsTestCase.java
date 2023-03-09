package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.partial;

import java.lang.reflect.Method;
import java.net.URLClassLoader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for https://issues.redhat.com/browse/JBMAR-167
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PartiallyKnownExceptionsTestCase {

    public static final String ARCHIVE_NAME = "partially-known-exceptions";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        archive.addClasses(
                Exception_A.class,
                Exception_B.class,
                BeanThrowingException.class,
                BeanThrowingExceptionRemote.class);
        return archive;
    }

    @BeforeClass
    public static void beforeClass() {
        //TODO
        Assume.assumeFalse("PartiallyKnownExceptionTestCase needs to be rewritten for JDK9+",
                TestEnvironment.JAVA_VERSION >= 11);

        Assume.assumeFalse("PartiallyKnownExceptionTestCase currently skipped because it intermittently hangs",
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTP) ||
                        TestEnvironment.getConnectorType().equals(ConnectorType.HTTPS));
    }

    /**
     * This is quite tricky to explain.
     *
     * Exception_B extends Exception_A.
     * The EJB's method is declared to throw Exception_A. It actually throws Exception_B,
     * but Exception_B is not known to the client (only Exception_A is). The expected outcome
     * is a ClassNotFoundException thrown from the EJB invocation (therefore wrapped in an EJBException).
     *
     * The classloader hack is to ensure that the test won't have Exception_B available (because normally it would have).
     * For this reason, we create a new URLClassLoader whose findClass is overridden so that it loads all classes normally, except
     * Exception_B. The test then runs in this new class loader.
     *
     * The actual logic of the test is therefore located in TestRunner.run()
     *
     * @throws Exception
     */
    @Test(timeout = 10000) // set timeout because this causes a hang for some reason intermittently
    // when running against https-remoting connector
    // TODO: investigate this ^
    public void testPartiallyKnownException() throws Exception {
        final URLClassLoader myClassLoader = new URLClassLoader(
                ((URLClassLoader)PartiallyKnownExceptionsTestCase.class.getClassLoader()).getURLs(),
                System.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if(name.contains("Exception_B"))
                    throw new ClassNotFoundException(name);
                return super.findClass(name);
            }
        };

        final Class<?> testRunnerClass = Class.forName(TestRunner.class.getName(), true, myClassLoader);
        final Object instance = testRunnerClass.newInstance();
        final Method run = testRunnerClass.getMethod("run");

        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(myClassLoader);
        try {
            run.invoke(instance);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTCCL);
        }
    }


}
