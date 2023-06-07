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

package org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author Jan Martiska
 */
public class GracefulShutdownHelper {

    public static final ModelNode OPERATION_SUSPEND;

    public static final ModelNode OPERATION_RESUME;

    static {
        OPERATION_SUSPEND = new ModelNode();
        OPERATION_SUSPEND.get(ClientConstants.OP).set("suspend");
        OPERATION_SUSPEND.protect();

        OPERATION_RESUME = new ModelNode();
        OPERATION_RESUME.get(ClientConstants.OP).set("resume");
        OPERATION_RESUME.protect();


    }
}
