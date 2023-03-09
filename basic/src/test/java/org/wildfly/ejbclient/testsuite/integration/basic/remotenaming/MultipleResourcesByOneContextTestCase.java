package org.wildfly.ejbclient.testsuite.integration.basic.remotenaming;

import jakarta.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateful;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateless;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test ability to access resources of various types (EJB, JMS) with one remote-naming context.
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleResourcesByOneContextTestCase {

    public static final String MODULE_NAME = "basic-echo-test";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        archive.addPackage(EchoBeanRemote.class.getPackage());
        return archive;
    }

    @BeforeClass
    public static void before() {
        Assume.assumeTrue(TestEnvironment.getContextType() == EJBClientContextType.WILDFLY_NAMING_CLIENT);
    }

    // TODO: do some plain naming lookups,.....
    @Test
    public void testMultipleServices() throws NamingException, JMSException {
        try(final InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final InitialContext initialContext = directory.getInitialContext();

            // lookup stateless ejb
            final EchoBeanRemote statelessBean = directory
                    .lookupStateless(MODULE_NAME, EchoBeanStateless.class, EchoBeanRemote.class);

            // lookup stateful ejb
            final EchoBeanRemote statefulBean = directory
                    .lookupStateful(MODULE_NAME, EchoBeanStateful.class, EchoBeanRemote.class);

            // invoke stateless ejb
            Assert.assertEquals("gg", statelessBean.echo("gg"));

            // invoke stateful ejb
            Assert.assertEquals("gg", statefulBean.echo("gg"));
        }


    }
}
