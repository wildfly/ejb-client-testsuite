package org.wildfly.ejbclient.testsuite.integration.basic.contextreferenceholding;

import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface StatefulCounter {

    Integer incrementAndGet();

}
