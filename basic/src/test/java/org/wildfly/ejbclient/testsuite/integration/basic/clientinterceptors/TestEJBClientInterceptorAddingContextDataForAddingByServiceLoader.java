package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.junit.Assert;

/**
 * @author Jan Martiska
 */
public class TestEJBClientInterceptorAddingContextDataForAddingByServiceLoader<T> implements EJBClientInterceptor {

    public static final AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);

    protected static final String DEFAULT_KEY = "default-key";
    protected static final WeirdCompoundValue DEFAULT_VALUE = new WeirdCompoundValue();

    private final String key;
    private final T value;

    public TestEJBClientInterceptorAddingContextDataForAddingByServiceLoader(String key, T value) {
        System.out.println(this.getClass().getName() + " being constructed with a custom constructor");
        this.key = key;
        this.value = value;
    }

    public TestEJBClientInterceptorAddingContextDataForAddingByServiceLoader() {
        System.out.println(this.getClass().getName() + " being constructed with the default constructor");
        this.value = (T)DEFAULT_VALUE;
        this.key = DEFAULT_KEY;
    }

    /**
     * This interceptor will be applied to all tests but should do anything
     * only in case when the test case is ClientInterceptorAddedViaServiceLoaderTestCase
     * @return
     */
    public boolean shouldRun() {
        final StackTraceElement[] stackTrace = new Exception().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            if(stackTraceElement.getClassName().contains("ClientInterceptorAddedViaServiceLoaderTestCase"))
                return true;
        }
        return false;
    }

    @Override
    public void handleInvocation(EJBClientInvocationContext ejbClientInvocationContext) throws Exception {
        if(shouldRun()) {
            INTERCEPTOR_CALLED.set(true);
            if (ejbClientInvocationContext.getInvokedMethod().getName().equals("returnContextData")) {
                System.out.println(this.getClass().getName() + " handing an invocation");
                ejbClientInvocationContext.getContextData().put(key, value);
            }
        }
        ejbClientInvocationContext.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext ejbClientInvocationContext)
            throws Exception {
        if(shouldRun()) {
            if (!ejbClientInvocationContext.getInvokedMethod().getName().equals("returnContextData"))
                return ejbClientInvocationContext.getResult();
            if (ejbClientInvocationContext.getContextData().containsKey("unknown"))
                return null;
            System.out.println(this.getClass().getName() + " handing an invocation result");
            final Object contextDataFromServer = ejbClientInvocationContext.getContextData().get("_" + key);
            System.out.println("CONTEXT DATA ON CLIENT SIDE:");
            ejbClientInvocationContext.getContextData().entrySet()
                    .forEach(entry -> System.out.println(entry.getKey() + " :: " + entry.getValue()));
            Assert.assertEquals(
                    "[issue WFLY-8449] Context data added by the server should be visible to client",
                    value,
                    contextDataFromServer);
        }
        return ejbClientInvocationContext.getResult();
    }

}
