package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.partial;

import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface BeanThrowingExceptionRemote {
    void doit() throws Exception_A;
}
