package org.wildfly.ejbclient.testsuite.integration.basic.io.bean;

import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface ThreadNameReturningBeanRemote {

    String getExecutorThreadName();

}
