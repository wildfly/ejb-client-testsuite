package org.wildfly.ejbclient.testsuite.integration.multinode.failover.beans;

import jakarta.ejb.Remote;

/**
 * @author Michal Vinkler
 */
@Remote
public interface CounterBean {

    int getCounter();

    int getCounterAndIncrement();

    String getNode();

    void setCounter(int counter);

    String hello();
}
