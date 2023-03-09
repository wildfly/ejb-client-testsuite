package org.wildfly.ejbclient.testsuite.integration.basic.io.bean;

import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
public class ThreadNameReturningBean implements ThreadNameReturningBeanRemote {

    @Override
    public String getExecutorThreadName() {
        return Thread.currentThread().getName();
    }

}
