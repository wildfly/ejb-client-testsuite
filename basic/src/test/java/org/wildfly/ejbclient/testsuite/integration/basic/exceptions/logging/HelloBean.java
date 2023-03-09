package org.wildfly.ejbclient.testsuite.integration.basic.exceptions.logging;

import jakarta.ejb.Stateless;

@Stateless
public class HelloBean implements Hello {

    public void sayHello() { }
}
