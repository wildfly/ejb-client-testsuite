package org.wildfly.ejbclient.testsuite.integration.multinode.contextdata;

import jakarta.ejb.Remote;

@Remote
public interface IntermediaryBeanAddingContextDataRemote {

    Object addContextDataCallAndReturnIt(String key, Object value) throws Exception;

}
