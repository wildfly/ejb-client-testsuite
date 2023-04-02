package org.wildfly.ejbclient.testsuite.integration.multinode.failover.beans;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.logging.Logger;

/**
 * @author Michal Vinkler
 */
@Stateful
public class CounterBeanStateful implements CounterBean {

    @Resource
    private SessionContext ctx;

    private static Logger logger = Logger.getLogger(CounterBeanStateful.class.getName());

    private int counter;

    public CounterBeanStateful() {
        counter = 0;
    }

    @Override
    public int getCounter() {
        return counter;
    }

    @Override
    public int getCounterAndIncrement() {
        return ++counter;
    }

    @Override
    public String getNode() {
        final String node = System.getProperty("jboss.node.name");
        logger.info("method getNode() being handled by node " + node);
        return node;
    }

    @Override
    public void setCounter(int counter) {
        this.counter = counter;
    }

    @Override
    public String hello() {
        return "method hello() invoked by user " + ctx.getCallerPrincipal().getName()
                + " on node " + getNode() + ", counter = " + getCounterAndIncrement();
    }
}
