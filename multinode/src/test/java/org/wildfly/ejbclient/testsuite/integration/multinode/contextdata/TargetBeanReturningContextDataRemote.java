package org.wildfly.ejbclient.testsuite.integration.multinode.contextdata;

import jakarta.ejb.Remote;

@Remote
public interface TargetBeanReturningContextDataRemote {

    Object returnContextData(String key);
}
