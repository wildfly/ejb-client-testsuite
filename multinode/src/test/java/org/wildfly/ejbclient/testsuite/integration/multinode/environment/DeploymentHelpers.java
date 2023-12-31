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

package org.wildfly.ejbclient.testsuite.integration.multinode.environment;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.wildfly.extras.creaper.commands.deployments.Deploy;
import org.wildfly.extras.creaper.commands.deployments.Undeploy;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

public class DeploymentHelpers {

    /**
     * Deploys an archive using the given Creaper client.
     * If the deployment already exists on the server, this operation does nothing.
     */
    public static void deploy(Archive<?> shrinkWrapArchive, OnlineManagementClient client)
            throws CommandFailedException {
        Deploy deployCommand = new Deploy.Builder(
                new ZipExporterImpl(shrinkWrapArchive).exportAsInputStream(),
                shrinkWrapArchive.getName(),
                true).build();
        try {
            client.apply(deployCommand);
        } catch(CommandFailedException cfe) {
            if(!cfe.getMessage().toLowerCase().contains("duplicate resource"))
                throw cfe;
        }
    }

    /**
     * Undeploy an application from the server using the given Creaper client.
     * If no such deployment exists, this operation does nothing (the server will throw
     * an error, but we will ignore it).
     */
    public static void undeploy(String archive, OnlineManagementClient client) throws CommandFailedException {
        Undeploy undeployCommand = new Undeploy.Builder(archive).build();
        try {
            client.apply(undeployCommand);
        } catch(CommandFailedException cfe) {
            if(!cfe.getMessage().toLowerCase().contains("not found"))
                throw cfe;
        }
    }

}
