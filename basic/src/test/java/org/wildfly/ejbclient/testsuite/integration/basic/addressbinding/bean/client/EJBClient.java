package org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.client;

import org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;

import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;

@Stateful
@TransactionAttribute(value=NOT_SUPPORTED)
public class EJBClient implements EJBClientRemote{

//    @EJB(lookup = "ejb:/source-ip-address-server/IpSourceAddressReturningBean!org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote?stateful")
//    private IpSourceAddressReturningBeanRemote beanRemote;

    @Override
    public String connectToRemoteBeanAndGetMyIp() throws Exception {
        return callBeanByLookup();
    }

    public String callBeanByLookup() throws Exception {
        Properties props = new Properties();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        InitialContext ctx = new InitialContext(props);
        IpSourceAddressReturningBeanRemote remoteBeanByLookup = (IpSourceAddressReturningBeanRemote) ctx
                .lookup("ejb:/source-ip-address-server/IpSourceAddressReturningBean!org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote?stateful");
        return remoteBeanByLookup.getSourceAddress();
    }
}
