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

package org.wildfly.ejbclient.testsuite.integration.basic.beanpool;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;

public class BeanPoolManagementTools {

    static PathAddress ejbSubsystemAddress = PathAddress
            .pathAddress(PathElement.pathElement("subsystem", "ejb3"));

    // TODO these operations can be implemented in creaper
    public static void createStrictMaxBeanInstancePool(ModelControllerClient client,
                                                       String name,
                                                       PoolDeriveSize deriveSize,
                                                       Integer maxPoolSize,
                                                       Long timeout,
                                                       TimeUnit timeoutUnit) throws IOException {
        Objects.requireNonNull(name);

        PathAddress beanInstancePoolAddress = ejbSubsystemAddress
                .append(PathElement.pathElement("strict-max-bean-instance-pool", name));
        ModelNode addOperation = new ModelNode();
        addOperation.get(OP).set(ClientConstants.ADD);
        addOperation.get(ADDRESS).set(beanInstancePoolAddress.toModelNode());

        if (deriveSize != null) {
            addOperation.get("derive-size").set(deriveSize.toString());
        }
        if (maxPoolSize != null) {
            addOperation.get("max-pool-size").set(maxPoolSize);
        }
        if (timeout != null) {
            addOperation.get("timeout").set(timeout);
        }
        if (timeoutUnit != null) {
            addOperation.get("timeout-unit").set(timeoutUnit.toString());
        }

        final ModelNode result = client.execute(addOperation);
        System.out.println("--------------- PRINTING RESULT");
        System.out.println(result.toJSONString(false));
        Assert.assertEquals("Operation not successful: " + addOperation.toJSONString(false)  + ", result= " + result.toJSONString(false),
                "success",
                result.get(ClientConstants.OUTCOME).asString());
    }

    public static void removeStrictMaxBeanInstancePool(ModelControllerClient client,
                                                       String name) throws IOException {
        Objects.requireNonNull(name);
        PathAddress beanInstancePoolAddress = ejbSubsystemAddress
                .append(PathElement.pathElement("strict-max-bean-instance-pool", name));
        ModelNode removeOperation = new ModelNode();
        removeOperation.get(OP).set(REMOVE_OPERATION);
        removeOperation.get(ADDRESS).set(beanInstancePoolAddress.toModelNode());
        removeOperation.get(ClientConstants.OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

        final ModelNode result = client.execute(removeOperation);
        Assert.assertEquals("Operation not successful: " + removeOperation.toJSONString(false) + ", result= " + result.toJSONString(false),
                "success",
                result.get(ClientConstants.OUTCOME).asString());
    }

}
