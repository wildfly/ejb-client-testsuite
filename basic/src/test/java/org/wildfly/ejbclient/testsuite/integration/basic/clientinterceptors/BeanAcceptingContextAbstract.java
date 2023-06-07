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
