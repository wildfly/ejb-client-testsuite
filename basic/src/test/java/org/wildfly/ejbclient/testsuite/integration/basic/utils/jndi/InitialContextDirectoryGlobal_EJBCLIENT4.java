package org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Provider;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors.TestEJBClientInterceptor;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ServerAuthenticationType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.wildfly.httpclient.ejb.HttpClientProvider;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;

import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.PASSWORD;
import static org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment.USERNAME;

/**
 *
 * TODO: perhaps use PROVIDER_URL instead of the programmatic ejb client context?
 *
 * @author Jan Martiska
 */
@SuppressWarnings("unused")
public class InitialContextDirectoryGlobal_EJBCLIENT4 extends InitialContextDirectory {

    private static volatile boolean alreadyConfigured = false;

    public InitialContextDirectoryGlobal_EJBCLIENT4() throws NamingException, URISyntaxException,
            IOException {
        if(!alreadyConfigured) {
            System.setProperty("org.jboss.ejb.client.view.annotation.scan.enabled",
                    "true");  // without this, CompressionHint for requests won't work

            initAuthenticationContext();
            initEjbClientContext();
            alreadyConfigured = true;
        }

        Properties initContextProps = new Properties();
        initContextProps.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        this.ctx = new InitialContext(initContextProps);
    }

    /**
     * When TS is configured to use LOCAL auth:
     * - the authentication context always tries to use LOCAL auth
     *
     * When TS is configured to use USER auth (with local auth disabled):
     * - when connecting by EJB client, use DIGEST-MD5 mechanism
     * - when connecting by management client, use LOCAL auth
     */
    private void initAuthenticationContext() {
        if(TestEnvironment.getAuthenticationType() == ServerAuthenticationType.LOCAL) {
            AuthenticationConfiguration common = AuthenticationConfiguration.empty()
                    .useProviders(() -> new Provider[] { new WildFlyElytronProvider() })
                    .setSaslMechanismSelector(SaslMechanismSelector.ALL);
            AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, common);
            AuthenticationContext.getContextManager().setGlobalDefault(context);
        } else {
            AuthenticationConfiguration conf_applicationRealm = AuthenticationConfiguration.empty()
                    .useProviders(() -> new Provider[] { new WildFlyElytronProvider() })
                    .setSaslMechanismSelector(SaslMechanismSelector.ALL)
                    .useRealm("ApplicationRealm")
                    .useName(USERNAME)
                    .usePassword(PASSWORD);
            AuthenticationConfiguration conf_managementRealm = AuthenticationConfiguration.empty()
                    .useProviders(() -> new Provider[] { new WildFlyElytronProvider() })
                    .setSaslMechanismSelector(SaslMechanismSelector.ALL)
                    .useRealm("ManagementRealm");

            AuthenticationContext context = AuthenticationContext.empty();
            context = context.with(MatchRule.ALL.matchPort(9990), conf_managementRealm);
            context = context.with(MatchRule.ALL.matchPort(TestEnvironment.getServerPort()), conf_applicationRealm);

            AuthenticationContext.getContextManager().setGlobalDefault(context);
        }
    }

    private void initEjbClientContext() throws IOException {
        final EJBClientConnection.Builder connectionBuilder = new EJBClientConnection.Builder();
        final URI uri = URI.create(TestEnvironment.getServerURL());
        connectionBuilder.setDestination(uri);

        final EJBClientContext.Builder builder = new EJBClientContext.Builder();
        builder.addInterceptor(new TestEJBClientInterceptor());
        builder.addClientConnection(connectionBuilder.build());
        builder.addTransportProvider(new RemoteTransportProvider());
        builder.addTransportProvider(new HttpClientProvider());

        final EJBClientContext ctx = builder.build();
        EJBClientContext.getContextManager().setGlobalDefault(ctx);
    }

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
    }

}
