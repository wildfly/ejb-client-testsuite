package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.io.Serializable;

/**
 * An EJB which will read the invocation context data passed by the client and return them back to the client.
 * @author Jan Martiska
 */
public interface BeanAcceptingContextData<T extends Serializable> {

    T returnContextData(String key);

}
