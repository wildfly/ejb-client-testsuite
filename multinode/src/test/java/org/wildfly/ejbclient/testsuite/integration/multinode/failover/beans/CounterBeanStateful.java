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

package org.wildfly.ejbclient.testsuite.integration.multinode.failover.beans;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.logging.Logger;

/**
 * @author Michal Vinkler
 */
@Stateful
public class CounterBeanStateful implements CounterBean {

    @Resource
    private SessionContext ctx;

    private static Logger logger = Logger.getLogger(CounterBeanStateful.class.getName());

    private int counter;

    public CounterBeanStateful() {
        counter = 0;
    }

    @Override
    public int getCounter() {
        return counter;
    }

    @Override
    public int getCounterAndIncrement() {
        return ++counter;
    }

    @Override
    public String getNode() {
        final String node = System.getProperty("jboss.node.name");
        logger.info("method getNode() being handled by node " + node);
        return node;
    }

    @Override
    public void setCounter(int counter) {
        this.counter = counter;
    }

    @Override
    public String hello() {
        return "method hello() invoked by user " + ctx.getCallerPrincipal().getName()
                + " on node " + getNode() + ", counter = " + getCounterAndIncrement();
    }
}
