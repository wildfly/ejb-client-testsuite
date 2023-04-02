package org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans;

import jakarta.ejb.Remote;

@Remote
public interface IntermediaryBeanRemote {

    void call();

}
