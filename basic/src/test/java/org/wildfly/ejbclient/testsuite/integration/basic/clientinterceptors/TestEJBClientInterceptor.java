package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

public class TestEJBClientInterceptor implements EJBClientInterceptor {

    public static final AtomicBoolean CALLED_FOR_INVOCATION = new AtomicBoolean(false);
    public static final AtomicBoolean CALLED_FOR_INVOCATION_RESULT = new AtomicBoolean(false);

    @Override
    public void handleInvocation(EJBClientInvocationContext ejbClientInvocationContext) throws Exception {
        CALLED_FOR_INVOCATION.set(true);
        ejbClientInvocationContext.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext ejbClientInvocationContext)
            throws Exception {
        CALLED_FOR_INVOCATION_RESULT.set(true);
        return ejbClientInvocationContext.getResult();
    }

    public static void reset() {
        CALLED_FOR_INVOCATION.set(false);
        CALLED_FOR_INVOCATION_RESULT.set(false);
    }

}
