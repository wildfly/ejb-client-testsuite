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

package org.wildfly.ejbclient.testsuite.integration.multinode.clusternodeselector;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.jboss.ejb.client.ClusterNodeSelector;

public class CustomClusterNodeSelector implements ClusterNodeSelector {

    private static Logger logger = Logger.getLogger(CustomClusterNodeSelector.class.getName());

    // node hint obtained from the test - the test tells the node selector what node should be picked
    // if this is null, the selector will select the first available node
    private static volatile String NODE_HINT;

    public static void setNodeHint(String nodeName) {
        NODE_HINT = nodeName;
    }

    @Override
    public String selectNode(String clusterName, String[] connectedNodes, String[] totalAvailableNodes) {
        logger.info("clusterName = " + clusterName);
        logger.info("connectedNodes = " + Arrays.toString(connectedNodes));
        logger.info("totalAvailableNodes = " + Arrays.toString(totalAvailableNodes));
        logger.info("node hint = " + NODE_HINT);
        if(NODE_HINT != null) {
            return NODE_HINT;
        } else {
            return connectedNodes[0];
        }
    }
}
