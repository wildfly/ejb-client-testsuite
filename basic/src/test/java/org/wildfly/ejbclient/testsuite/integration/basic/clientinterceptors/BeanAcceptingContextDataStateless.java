package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.io.Serializable;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
@Remote(BeanAcceptingContextData.class)
public class BeanAcceptingContextDataStateless<T extends Serializable> extends BeanAcceptingContextAbstract<T> {
}
