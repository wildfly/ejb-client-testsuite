package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.io.Serializable;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

/**
 * @author Jan Martiska
 */
@Stateful
@Remote(BeanAcceptingContextData.class)
public class BeanAcceptingContextDataStateful<T extends Serializable> extends BeanAcceptingContextAbstract<T> {

}
