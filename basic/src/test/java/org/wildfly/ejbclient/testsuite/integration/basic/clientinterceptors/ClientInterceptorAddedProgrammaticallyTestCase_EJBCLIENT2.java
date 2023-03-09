package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import jakarta.ejb.EJBException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.tests.clientinterceptors.unknown.UnknownSerializableClass;
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

/**
 * The client passes invocation context data to the server using a client-side interceptor.
 * The server side bean is supposed to read the context data and return it back to the client.
 * WeirdCompoundValue is used as the class for the passed context data.
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorAddedProgrammaticallyTestCase_EJBCLIENT2 {

    public static final String MODULE_NAME = "client-interceptor";

    private InitialContextDirectory ctxDirectory;

    @Deployment(name = MODULE_NAME, testable = false)
    public static JavaArchive deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(ClientInterceptorAddedProgrammaticallyTestCase_EJBCLIENT2.class.getPackage());
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        // only for non-scoped EJB client contexts
        Assume.assumeTrue(TestEnvironment.getContextType() == EJBClientContextType.GLOBAL);
    }

    @Before
    public void prepare() {
        ctxDirectory = new InitialContextDirectory.Supplier().get();
        TestEJBClientInterceptorAddingContextData.INTERCEPTOR_CALLED.set(false);
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
        WeirdCompoundValue original = new WeirdCompoundValue();
        EJBClientContext.getCurrent().registerInterceptor(1,
                new TestEJBClientInterceptorAddingContextData<WeirdCompoundValue>("key", original));
        WeirdCompoundValue returned = instance.returnContextData("key");
        Assert.assertNotNull("Server did not return context data", returned);
        Assert.assertEquals("Server returned context data, but it is corrupt", original, returned);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doStateless() throws NamingException {
        Object o = ctxDirectory.lookupStateless(MODULE_NAME, BeanAcceptingContextDataStateless.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        WeirdCompoundValue original = new WeirdCompoundValue();
        final EJBClientInterceptor.Registration registration = EJBClientContext.getCurrent().registerInterceptor(1,
                new TestEJBClientInterceptorAddingContextData<WeirdCompoundValue>("key", original));
        try {
            WeirdCompoundValue returned = instance.returnContextData("key");
            Assert.assertNotNull("Server did not return context data", returned);
            Assert.assertEquals("Server returned context data, but it is corrupt", original, returned);
        } finally {
            registration.remove();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doSingleton() throws NamingException {
        Object o = ctxDirectory.lookupStateless(MODULE_NAME, BeanAcceptingContextDataSingleton.class,
                BeanAcceptingContextData.class);
        BeanAcceptingContextData<WeirdCompoundValue> instance
                = (BeanAcceptingContextData<WeirdCompoundValue>)o;
        WeirdCompoundValue original = new WeirdCompoundValue();
        final EJBClientInterceptor.Registration registration = EJBClientContext.requireCurrent().registerInterceptor(1,
                new TestEJBClientInterceptorAddingContextData<WeirdCompoundValue>("key", original));
        try {
            WeirdCompoundValue returned = instance.returnContextData("key");
            Assert.assertNotNull("Server did not return context data", returned);
            Assert.assertEquals("Server returned context data, but it is corrupt", original, returned);
        } finally {
            registration.remove();
        }
    }

    /**
     *  Try passing an instance of a class that the application's class loader doesn't know about.
     *
     *  Current behavior is that the EJB call will fail during unmarshalling the request (on the server side), an EJBException is returned to the client.
     *  Whether this behavior is optimal, I don't know.
     *  The test will also pass if no exception is thrown. So it just basically checks that nothing really unexpected happens (like a deadlock for example).
     *  It will fail if an exception other than EJBException is thrown.
     */
    @Test(timeout = 5000)
    public void unknownClass() throws NamingException {
        final BeanAcceptingContextData<UnknownSerializableClass> bean = ctxDirectory
                .lookupStateless(MODULE_NAME, BeanAcceptingContextDataSingleton.class,
                        BeanAcceptingContextData.class);
        final EJBClientInterceptor.Registration registration = EJBClientContext.requireCurrent()
                .registerInterceptor(1, new TestEJBClientInterceptorAddingContextData<>("unknown",
                        new UnknownSerializableClass()));
        try {
            final UnknownSerializableClass ret = bean.returnContextData("unknown");
        } catch (EJBException e) {
            // ok
        } finally {
            registration.remove();
        }
    }

}
