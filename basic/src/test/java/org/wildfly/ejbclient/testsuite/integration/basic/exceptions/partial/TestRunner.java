package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.partial;

import jakarta.ejb.EJBException;

import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.junit.Assert;

/**
 * @author Jan Martiska
 */
public class TestRunner implements Runnable {

    @Override
    public void run() {
        System.out.println("Running in classloader: " + TestRunner.class.getClassLoader());
        final InitialContextDirectory directory = new InitialContextDirectory.Supplier().get();
        final BeanThrowingExceptionRemote bean;
        try {
            bean = directory
                    .lookupStateless(PartiallyKnownExceptionsTestCase.ARCHIVE_NAME,
                            BeanThrowingException.class,
                            BeanThrowingExceptionRemote.class);
            bean.doit();
        } catch (EJBException e) {
            e.printStackTrace();
            Assert.assertEquals(
                    "Expected an EJBException caused by a ClassNotFoundException but the cause was: ",
                    ClassNotFoundException.class, e.getCause().getClass());
            return;
        } catch (Exception e) {
            throw new RuntimeException("Expected an EJBException caused by a ClassNotFoundException, but received", e);
        }
        throw new RuntimeException("Expected an EJBException caused by a ClassNotFoundException, but didn't get any exception");
    }
}
