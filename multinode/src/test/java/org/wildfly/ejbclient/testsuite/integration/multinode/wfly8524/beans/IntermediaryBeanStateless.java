package org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import org.jboss.logging.Logger;

@Stateless
public class IntermediaryBeanStateless implements IntermediaryBeanRemote {

    private static Logger logger = Logger.getLogger(IntermediaryBeanStateless.class.getName());
    
    @EJB(lookup = "ejb:/bean-target/TargetBeanStateful!org.wildfly.ejbclient.testsuite.integration.multinode.wly8524.beans.TargetBeanRemote?stateful")
    private TargetBeanRemote remote;

    @Override
    public void call() {
        logger.info("IntermediaryBeanStateless invoked on node " + System.getProperty("jboss.node.name"));
        remote.incrementAndGet();
    }

}
