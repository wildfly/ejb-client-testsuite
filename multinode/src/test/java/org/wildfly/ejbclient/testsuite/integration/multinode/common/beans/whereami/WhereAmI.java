package org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whereami;

import jakarta.ejb.Remote;

@Remote
public interface WhereAmI {

    default String getNode() {
        return System.getProperty("jboss.node.name");
    }
}
