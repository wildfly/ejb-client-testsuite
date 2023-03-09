package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.io.Serializable;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;

/**
 * @author Jan Martiska
 */
public class BeanAcceptingContextAbstract<T extends Serializable> implements BeanAcceptingContextData<T> {

    @Resource
    protected SessionContext ctx;

    @Override
    @SuppressWarnings("unchecked")
    public T returnContextData(String key) {
        final T value = (T)ctx.getContextData().get(key);
        ctx.getContextData().put("_" + key, value);
        System.out.println("CONTEXT DATA ON SERVER SIDE:");
        ctx.getContextData().entrySet().forEach(entry -> System.out.println(entry.getKey() + " :: " + entry.getValue()));
        System.out.println("Returning ::: " + value);
        return value;
    }

}
