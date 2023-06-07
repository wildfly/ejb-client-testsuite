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

package org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateful;

import org.jboss.logging.Logger;

@Stateful
public class TargetBeanStateful implements TargetBeanRemote {

    private static Logger logger = Logger.getLogger(TargetBeanStateful.class.getName());

    private AtomicInteger counter;

    @PostConstruct
    public void init() {
        counter = new AtomicInteger(0);
    }

    @Override
    public Integer incrementAndGet() {
        final int result = counter.incrementAndGet();
        logger.info("*** TargetBeanStateful invoked on node " + System.getProperty("jboss.node.name")
                + ", counter = " + result);
        return result;
    }

}
