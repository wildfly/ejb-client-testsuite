package org.wildfly.ejbclient.testsuite.integration.basic.lambda;

import java.io.Serializable;
import java.util.function.Supplier;
import jakarta.ejb.EJBException;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Client calls an EJB passing a lambda expression to the server as an argument.
 * @author Jan Martiska
 */
@SuppressWarnings("unchecked")
@RunAsClient
@RunWith(Arquillian.class)
public class LambdaTestCase {

    public static final String MODULE_NAME = "lambda-test";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        archive.addClasses(LambdaBean.class, LambdaRemote.class);
        return archive;
    }

    @Test
    public void test() throws NamingException {
        final InitialContextDirectory ctxDir = new InitialContextDirectory.Supplier().get();
        final LambdaRemote<String> lambdaRemote = ctxDir
                .lookupStateless(MODULE_NAME, LambdaBean.class, LambdaRemote.class);
        final String returned = lambdaRemote.evaluateAndReturn((Supplier<String> & Serializable)() -> "lolz");
        Assert.assertEquals("lolz", returned);
    }

    /**
     * This time the lambda's underlying bytecode is not present on the server.
     * This should crash on a ClassNotFoundException or so.
     */
    @Test
    public void testWithBytecodeNotPresentOnServer() throws NamingException {
        final InitialContextDirectory ctxDir = new InitialContextDirectory.Supplier().get();
        final LambdaRemote<String> lambdaRemote = ctxDir
                .lookupStateless(MODULE_NAME, LambdaBean.class, LambdaRemote.class);
        try {
            final String returned = HelperBean.invoke(lambdaRemote);
            Assert.fail("The call shouldn't proceed correctly, because the lambda's bytecode is not present on the server");
        } catch(EJBException ex) {
            // ok
            Assert.assertTrue("expected ClassNotFoundException, but was: " + ex.getCausedByException().getClass(), ex.getCausedByException() instanceof ClassNotFoundException);
        }
    }

}
