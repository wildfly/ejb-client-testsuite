package org.wildfly.ejbclient.testsuite.integration.basic.echo;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateless;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatelessEchoTestCase extends BasicEchoAbstractTestCase {

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "basic-echo-test.war");
        archive.addPackage(EchoBeanRemote.class.getPackage());
        return archive;
    }

    @Override
    protected Class<? extends EchoBeanRemote> getBeanClass() {
        return EchoBeanStateless.class;
    }

    @Override
    protected BeanType getBeanType() {
        return BeanType.STATELESS;
    }
}
