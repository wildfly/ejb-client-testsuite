package org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.timerservice;

import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface TimedCountingBeanRemote {

    int getTicks();

}
