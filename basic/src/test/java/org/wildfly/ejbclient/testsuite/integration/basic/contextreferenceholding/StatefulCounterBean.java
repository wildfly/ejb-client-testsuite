package org.wildfly.ejbclient.testsuite.integration.basic.contextreferenceholding;

import jakarta.ejb.Stateful;

/**
 * @author Jan Martiska
 */
@Stateful
public class StatefulCounterBean implements StatefulCounter {

    private Integer counter = 0;

/*    @PostConstruct
    public void init() {
        counter = 0;
    }*/

    @Override
    public Integer incrementAndGet() {
        return counter++;
    }
}
