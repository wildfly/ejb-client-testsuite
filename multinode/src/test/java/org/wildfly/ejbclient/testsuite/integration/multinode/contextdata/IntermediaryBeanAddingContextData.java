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

package org.wildfly.ejbclient.testsuite.integration.multinode.contextdata;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

@Stateless
public class IntermediaryBeanAddingContextData implements IntermediaryBeanAddingContextDataRemote {

    private static Logger logger = Logger.getLogger(IntermediaryBeanAddingContextData.class.getName());
    @EJB(lookup = "ejb:/bean-target/TargetBeanReturningContextData!org.wildfly.ejbclient.testsuite.integration.multinode.contextdata.TargetBeanReturningContextDataRemote")
    private TargetBeanReturningContextDataRemote targetBean;

    @Override
    public Object addContextDataCallAndReturnIt(String key, Object value) throws Exception {
        final CompletableFuture<Object> callResult = new CompletableFuture<>();
        EJBClientContext.getCurrent().withAddedInterceptors(new EJBClientInterceptor() {
            @Override
            public void handleInvocation(EJBClientInvocationContext context) throws Exception {
                logger.info(
                        "**** IntermediaryBeanAddingContextData is adding context data using a client interceptor");
                context.getContextData().put(key, value);
                context.sendRequest();
            }

            @Override
            public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
                return context.getResult();
            }
        }).run(() -> callResult.complete(targetBean.returnContextData(key)));
        return callResult.get(15, TimeUnit.SECONDS);
    }
}
