package org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.client;

import jakarta.ejb.Remote;
import javax.naming.NamingException;
import java.io.IOException;

@Remote
public interface EJBClientRemote {
    public String connectToRemoteBeanAndGetMyIp() throws IOException, NamingException, Exception;
}
