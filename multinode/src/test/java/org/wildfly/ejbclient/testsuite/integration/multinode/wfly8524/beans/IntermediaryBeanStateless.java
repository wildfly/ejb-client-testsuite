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

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import org.jboss.logging.Logger;

@Stateless
public class IntermediaryBeanStateless implements IntermediaryBeanRemote {

    private static Logger logger = Logger.getLogger(IntermediaryBeanStateless.class.getName());
    
    @EJB(lookup = "ejb:/bean-target/TargetBeanStateful!org.wildfly.ejbclient.testsuite.integration.multinode.wfly8524.beans.TargetBeanRemote?stateful")
    private TargetBeanRemote remote;

    @Override
    public void call() {
        logger.info("IntermediaryBeanStateless invoked on node " + System.getProperty("jboss.node.name"));
        remote.incrementAndGet();
    }

}
