package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.logging;

import jakarta.ejb.Remote;

@Remote
public interface Hello {

    void sayHello();
}
