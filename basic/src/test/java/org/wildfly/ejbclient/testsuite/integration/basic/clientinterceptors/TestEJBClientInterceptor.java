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
