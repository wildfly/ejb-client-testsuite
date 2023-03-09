package org.wildfly.ejbclient.testsuite.integration.basic.echo.async;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.async.echobean.EchoBeanAsyncRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.async.echobean.EchoBeanAsyncStateful;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AsyncStatefulEchoTestCase extends AsyncEchoAbstractTestCase {

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, AsyncEchoAbstractTestCase.MODULE_NAME + ".war");
        archive.addPackage(EchoBeanAsyncRemote.class.getPackage());
        return archive;
    }

    @Override
    protected Class<? extends EchoBeanAsyncRemote> getBeanClass() {
        return EchoBeanAsyncStateful.class;
    }

    @Override
    protected BeanType getBeanType() {
        return BeanType.STATEFUL;
    }
}
