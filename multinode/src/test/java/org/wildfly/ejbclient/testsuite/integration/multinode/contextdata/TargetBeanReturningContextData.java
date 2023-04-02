package org.wildfly.ejbclient.testsuite.integration.multinode.contextdata;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

@Stateless
public class TargetBeanReturningContextData implements TargetBeanReturningContextDataRemote {

    @Resource
    private SessionContext ctx;

    @Override
    public Object returnContextData(String key) {
        return ctx.getContextData().get(key);
    }

}
