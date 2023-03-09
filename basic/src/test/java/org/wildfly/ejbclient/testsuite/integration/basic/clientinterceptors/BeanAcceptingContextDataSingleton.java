package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.io.Serializable;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

/**
 * @author Jan Martiska
 */
@Singleton
@Remote(BeanAcceptingContextData.class)
public class BeanAcceptingContextDataSingleton<T extends Serializable>  extends BeanAcceptingContextAbstract<T> {

}
