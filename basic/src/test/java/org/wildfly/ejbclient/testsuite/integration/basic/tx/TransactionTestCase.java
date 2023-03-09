package org.wildfly.ejbclient.testsuite.integration.basic.tx;

import javax.naming.NamingException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;


import org.jboss.ejb.client.EJBClient;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TransactionTestCase {

    public static final String DEPLOYMENT_NAME = "transaction-test";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        webArchive.addPackage(TransactionalBean.class.getPackage());
        webArchive.addAsWebInfResource(ClassLoader.getSystemResource("persistence.xml"),
                "classes/META-INF/persistence.xml");
        return webArchive;
    }

    @BeforeClass
    public static void onlyForGlobalContexts() {
        Assume.assumeTrue(TestEnvironment.getContextType().equals(EJBClientContextType.GLOBAL));
    }

    @Before
    public void cleanup() throws NamingException {
        try(final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final TransactionalBeanRemote bean = ctx
                    .lookupStateless(DEPLOYMENT_NAME, TransactionalBean.class, TransactionalBeanRemote.class);
            bean.clean();
        }
    }

    @Test
    public void testCommit()
            throws NamingException, SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException {
        final UserTransaction tx = EJBClient.getUserTransaction(TestEnvironment.NODE_NAME);
        try(final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final TransactionalBeanRemote bean = ctx
                    .lookupStateless(DEPLOYMENT_NAME, TransactionalBean.class, TransactionalBeanRemote.class);
            tx.begin();
            bean.createPerson();
            tx.commit();
            Assert.assertEquals("There should be a person in the DB, the transaction was committed", 1, bean.getPersonList().size());
        }
    }

    @Test
    public void testRollback()
            throws NamingException, SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException {
        final UserTransaction tx = EJBClient.getUserTransaction(TestEnvironment.NODE_NAME);
        try(final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            final TransactionalBeanRemote bean = ctx
                    .lookupStateless(DEPLOYMENT_NAME, TransactionalBean.class, TransactionalBeanRemote.class);
            tx.begin();
            bean.createPerson();
            tx.rollback();
            Assert.assertTrue("There should be nothing in the DB, the transaction was rolled back", bean.getPersonList().isEmpty());
        }
    }
}
