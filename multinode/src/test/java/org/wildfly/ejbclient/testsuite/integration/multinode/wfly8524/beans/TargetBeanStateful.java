package org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateful;

import org.jboss.logging.Logger;

@Stateful
public class TargetBeanStateful implements TargetBeanRemote {

    private static Logger logger = Logger.getLogger(TargetBeanStateful.class.getName());

    private AtomicInteger counter;

    @PostConstruct
    public void init() {
        counter = new AtomicInteger(0);
    }

    @Override
    public Integer incrementAndGet() {
        final int result = counter.incrementAndGet();
        logger.info("*** TargetBeanStateful invoked on node " + System.getProperty("jboss.node.name")
                + ", counter = " + result);
        return result;
    }

}
