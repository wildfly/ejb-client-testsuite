package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.partial;

import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
public class BeanThrowingException implements BeanThrowingExceptionRemote {

    @Override
    public void doit() throws Exception_A {
        System.out.println("Throwing an EXCEPTION");
        throw new Exception_B();
    }
}
