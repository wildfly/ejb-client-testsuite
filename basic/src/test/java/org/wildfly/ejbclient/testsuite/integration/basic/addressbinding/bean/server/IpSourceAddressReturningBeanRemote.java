package org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server;

import jakarta.ejb.Remote;

@Remote
public interface IpSourceAddressReturningBeanRemote {
    public String getSourceAddress() throws InterruptedException, Exception;
}
