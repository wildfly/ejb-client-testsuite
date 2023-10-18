/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.junit.Assert;

/**
 * @author Jan Martiska
 */
public class TestEJBClientInterceptorAddingContextData<T> implements EJBClientInterceptor {

    public static final AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);

    protected static final String DEFAULT_KEY = "default-key";
    protected static final WeirdCompoundValue DEFAULT_VALUE = new WeirdCompoundValue();

    private final String key;
    private final T value;

    public TestEJBClientInterceptorAddingContextData(String key, T value) {
        System.out.println(this.getClass().getName() + " being constructed with a custom constructor");
        this.key = key;
        this.value = value;
    }

    public TestEJBClientInterceptorAddingContextData() {
        System.out.println(this.getClass().getName() + " being constructed with the default constructor");
        this.value = (T)DEFAULT_VALUE;
        this.key = DEFAULT_KEY;
    }

    @Override
    public void handleInvocation(EJBClientInvocationContext ejbClientInvocationContext) throws Exception {
        INTERCEPTOR_CALLED.set(true);
        if(ejbClientInvocationContext.getInvokedMethod().getName().equals("returnContextData")) {
            System.out.println(this.getClass().getName() + " handing an invocation");
            /**
             *  TODO
             *  We need to add _key (and not key) to jboss.returned.keys cause server interceptor modifies it that way
             *  to make sure that we are returning the value actually produced on the server. This is OK, but imho we
             *  may think about making some modifications here (naming or further comments?) so that the test is more
             *  self-explanatory for someone who jumps to debug it.
             */
            ejbClientInvocationContext.addReturnedContextDataKey("_" + key);
            ejbClientInvocationContext.getContextData().put(key, value);
        }
        ejbClientInvocationContext.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext ejbClientInvocationContext)
            throws Exception {
        if(!ejbClientInvocationContext.getInvokedMethod().getName().equals("returnContextData"))
            return ejbClientInvocationContext.getResult();
        if(ejbClientInvocationContext.getContextData().containsKey("unknown"))
            return null;
        /**
         * It is necessary for the result to be calculated before perfoming any checks cause it is getResult methods
         * that triggers invocation of the code that is responsible for returned context calculation.
         */
        Object result = ejbClientInvocationContext.getResult();
        System.out.println(this.getClass().getName() + " handing an invocation result");
        final Object contextDataFromServer = ejbClientInvocationContext.getContextData().get("_" + key);

        System.out.println("CONTEXT DATA ON CLIENT SIDE:");
        ejbClientInvocationContext.getContextData().entrySet().forEach(entry -> System.out.println(entry.getKey() + " :: " + entry.getValue()));
        Assert.assertEquals(
                "[issue WFLY-8449] Context data added by the server should be visible to client", value,
                contextDataFromServer);
        return result;
    }

}
