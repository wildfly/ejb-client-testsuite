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

package org.wildfly.ejbclient.testsuite.integration.basic.utils;


import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper for management commands.
 *
 * This class is currently using Management API Client. If more complex things are needed, using creaper should be considered.
 */
public class ModelUtil {

    private static final Logger log = Logger.getLogger(ModelUtil.class.getName());

    public static List<String> modelNodeAsStringList(ModelNode node) {
        List<String> ret = new LinkedList<String>();
        for (ModelNode n : node.asList())
            ret.add(n.asString());
        return ret;
    }

    public static ModelNode createCompositeNode(ModelNode... steps) {
        ModelNode comp = new ModelNode();
        comp.get("operation").set("composite");
        for (ModelNode step : steps) {
            comp.get("steps").add(step);
        }
        return comp;
    }

    public static boolean execute(ManagementClient client, ModelNode operation) {
        try {
            ModelNode result = client.getControllerClient().execute(operation);
            return getOperationResult(result);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed execution of operation " + operation.toJSONString(false), e);
            return false;
        }
    }

    private static boolean getOperationResult(ModelNode node) {

        boolean success = "success".equalsIgnoreCase(node.get("outcome").asString());
        if (!success) {
            log.log(Level.WARNING, "Operation failed with \n{0}", node.toJSONString(false));
        }

        return success;

    }

    public static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String[] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }
}