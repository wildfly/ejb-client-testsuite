package org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server;


import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;

import java.util.concurrent.CountDownLatch;

import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;

@Stateful
@TransactionAttribute(value=NOT_SUPPORTED)
public class IpSourceAddressReturningBean implements IpSourceAddressReturningBeanRemote {

    @Resource
    SessionContext ctx;

    @Override
    public String getSourceAddress() throws Exception {
        Object o = ctx.getContextData().get("jboss.source-address");
        System.out.println("=====>>> Client's address: " + (o == null ? "null" : o.toString()));
//        latch.await();
        return o == null ? "null" : o.toString();
    }
}
