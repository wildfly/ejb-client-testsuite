package org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.qa.ejbclient.utils.ServerAuthenticationType;
import org.jboss.qa.ejbclient.utils.TestEnvironment;

import static org.jboss.qa.ejbclient.utils.ConnectorType.HTTPS_REMOTING;
import static org.jboss.qa.ejbclient.utils.ConnectorType.REMOTING_SSL;
import static org.jboss.qa.ejbclient.utils.TestEnvironment.PASSWORD;
import static org.jboss.qa.ejbclient.utils.TestEnvironment.USERNAME;
import static org.jboss.qa.ejbclient.utils.TestEnvironment.getConnectorType;

/**
 * @author Jan Martiska
 */
@SuppressWarnings("unused")
public class InitialContextDirectoryGlobal_EJBCLIENT2 extends InitialContextDirectory {

    public InitialContextDirectoryGlobal_EJBCLIENT2() throws NamingException {
        System.setProperty("org.jboss.ejb.client.view.annotation.scan.enabled", "true");  // without this, CompressionHint for requests won't work
        Properties ejbContextProps = new Properties();
        ejbContextProps.put("remote.connections", "main");
        ejbContextProps.put("remote.connection.main.protocol",
                TestEnvironment.getConnectorType().getConnectionScheme());
        ejbContextProps.put("remote.connection.main.port", Integer.toString(TestEnvironment.getServerPort()));
        ejbContextProps.put("remote.connection.main.host", "127.0.0.1");
        ejbContextProps.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS",
                "false");
        ejbContextProps.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT",
                "false");
        ejbContextProps.put("remote.connection.main.connect.options.org.xnio.Options.SSL_STARTTLS", "true");
        ejbContextProps.put("remote.connection.main.connect.options.org.xnio.Options.SSL_ENABLED",
                Boolean.toString(getConnectorType().equals(HTTPS_REMOTING) | getConnectorType().equals(REMOTING_SSL)));
        if (TestEnvironment.getAuthenticationType() == ServerAuthenticationType.USER) {
            ejbContextProps.put("remote.connection.main.username", USERNAME);
            ejbContextProps.put("remote.connection.main.password", PASSWORD);
            ejbContextProps
                    .put("remote.connection.main.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS",
                            "JBOSS-LOCAL-USER");
        }

        EJBClientConfiguration cc = new PropertiesBasedEJBClientConfiguration(ejbContextProps);
        ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(cc, Thread.currentThread().getContextClassLoader());
        previousSelector = EJBClientContext
                .setSelector(selector);

        Properties initContextProps = new Properties();
        initContextProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        ctx = new InitialContext(initContextProps);
    }

    private final ContextSelector<EJBClientContext> previousSelector;

    @Override
    public <T> T lookup(String applicationName, String moduleName, Class<? extends T> beanClass,
                        Class<T> beanInterface,
                        BeanType beanType, String distinctName) throws NamingException {

        applicationName = applicationName == null ? "" : applicationName;
        String name = createJndiName(applicationName, moduleName, beanClass, beanInterface, beanType,
                distinctName);
        return getBean(name, beanInterface);
    }

    public <T> String createJndiName(String earName, String jarName, Class<?> beanClass,
                                     Class<T> beanInterface, BeanType type, String distinctName) {
        return String.format("ejb:%s/%s/%s/%s!%s%s",
                earName, jarName, distinctName == null ? "" : distinctName, beanClass.getSimpleName(),
                beanInterface.getName(),
                (type == BeanType.STATEFUL) ? "?stateful" : "");
    }

    @Override
    public void close() {
        EJBClientContext.setSelector(previousSelector);
    }

}
