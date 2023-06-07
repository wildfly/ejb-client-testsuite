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

import java.io.File;
import java.io.IOException;

import com.google.common.base.Strings;

import org.apache.log4j.Logger;
import org.jboss.dmr.ModelNode;
import org.wildfly.extras.creaper.commands.logging.ChangeConsoleLogHandler;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineOptions;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;

public class ManagementHelpers {

    private static Logger logger = Logger.getLogger(ManagementHelpers.class.getName());

    public static final Address JGROUPS_EE_CHANNEL_RESOURCE = Address.subsystem("jgroups").and("channel", "ee");

    public static OnlineManagementClient createCreaper(String nodeHostname, Integer port) {
        return ManagementClient
                .onlineLazy(OnlineOptions.standalone().hostAndPort(nodeHostname, port).build());
    }

    public static void safeClose(OnlineManagementClient... clients) {
        for (OnlineManagementClient client : clients) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * /subsystem=elytron/authentication-configuration=NAME
     *      :add(credential-reference={clear-text="PASSWORD"}, authentication-name=USERNAME, realm=ApplicationRealm)
     * /subsystem=elytron/authentication-context=NAME:add(match-rules=[{authentication-configuration=NAME}])
     * TODO implement a creaper command for this and replace this code with it
     */
    public static void createAuthenticationContext(OnlineManagementClient creaper, String name,
                                                   String username, String password) throws IOException {
        new Operations(creaper).add(
                Address.subsystem("elytron")
                    .and("authentication-configuration", name),
                Values.ofObject("credential-reference", Values.of("clear-text", password))
                    .and("authentication-name", username)
                    .and("realm", "ApplicationRealm")
        );
        new Operations(creaper).add(
                Address.subsystem("elytron")
                        .and("authentication-context", name),
                Values.ofList("match-rules", ModelNode.fromJSONString("{\"authentication-configuration\":\""+name+"\"}"))
        );
    }

    /**
     * /subsystem=elytron/authentication-configuration=NAME:remove
     * /subsystem=elytron/authentication-context=NAME:remove*
     */
    public static void removeAuthenticationContext(OnlineManagementClient creaper, String name)
            throws Exception {
        new Operations(creaper).removeIfExists(
                Address.subsystem("elytron")
                        .and("authentication-context", name)
        );
        new Operations(creaper).removeIfExists(
                Address.subsystem("elytron")
                        .and("authentication-configuration", name));
    }

    /**
     * Creates a remote outbound connection, including an outbound socket binding of the same name.
     */
    public static void createRemoteOutboundConnection(OnlineManagementClient creaper, String name,
                                                      String destinationHost,
                                                      int destinationPort, String authenticationContextName) throws IOException {
        // TODO implement a creaper command for this and replace this code with it
        // TODO this should also accept authentication-context (and perhaps security-realm as well)
        logger.info("Creating remote-outbound-connection ("
                + "name=" + name + ", "
                + "destinationHost=" + destinationHost + ", "
                + "destinationPort=" + destinationPort
                + ")");
        createOutboundSocketBinding(creaper, name, destinationHost, destinationPort);
        new Operations(creaper).add(
                Address.subsystem("remoting")
                        .and("remote-outbound-connection", name),
                Values.of("outbound-socket-binding-ref", name)
                    .andOptional("authentication-context", authenticationContextName)
        );

    }

    public static void removeRemoteOutboundConnection(OnlineManagementClient creaper, String name)
            throws IOException {
        // TODO implement a creaper command for this and replace this code with it
        new Operations(creaper).remove(
                Address.subsystem("remoting")
                        .and("remote-outbound-connection", name)
        );
        removeOutboundSocketBinding(creaper, name);
    }

    private static void createOutboundSocketBinding(OnlineManagementClient creaper, String name,
                                                    String destinationHost, int destinationPort)
            throws IOException {
        // TODO implement a creaper command for this and replace this code with it
        new Operations(creaper).add(
                Address.of("socket-binding-group", "standard-sockets")
                        .and("remote-destination-outbound-socket-binding", name),
                Values.of("host", destinationHost)
                        .and("port", destinationPort)
        );
    }

    private static void removeOutboundSocketBinding(OnlineManagementClient creaper, String name)
            throws IOException {
        // TODO implement a creaper command for this and replace this code with it
        new Operations(creaper).remove(
                Address.of("socket-binding-group", "standard-sockets")
                        .and("remote-destination-outbound-socket-binding", name)
        );
    }

    public static void setLoggerPrefix(String prefix, String jbossHome, String configurationFile)
            throws IOException, CommandFailedException {
        final OfflineManagementClient offlineCreaper = ManagementClient.offline(
                OfflineOptions.standalone().baseDirectory(new File(jbossHome + File.separator + "standalone"))
                        .configurationFile(configurationFile).build());
        String actualPrefix = Strings.isNullOrEmpty(prefix) ? "" : (prefix + ": ");
        offlineCreaper.apply(
                new ChangeConsoleLogHandler.Builder("CONSOLE")
                        .patternFormatter(actualPrefix + "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n").build()
        );
    }

    public static void setConsoleHandlerLevel(OnlineManagementClient creaper, String level)
            throws IOException, CommandFailedException {
        new Operations(creaper).writeAttribute(Address.subsystem("logging").and("console-handler", "CONSOLE"), "level", level);
    }

    public static void createLoggerCategory(OnlineManagementClient creaper, String category, String level)
            throws IOException, CommandFailedException {
        new Operations(creaper).add(Address.subsystem("logging").and("logger", category), Values.of("level", level));
    }

    public static void removeLoggerCategory(OnlineManagementClient creaper, String category)
            throws IOException, CommandFailedException, OperationException {
        new Operations(creaper).removeIfExists(Address.subsystem("logging").and("logger", category));
    }

    public static String setClusterAttributeOfJGroupsEEChannel(OnlineManagementClient client, String newValue)
            throws IOException {
        final ModelNodeResult oldValueResult = new Operations(client)
                .readResource(JGROUPS_EE_CHANNEL_RESOURCE);
        final String oldValue = oldValueResult.get("cluster").asStringOrNull();
        if(newValue != null) {
            new Operations(client).writeAttribute(JGROUPS_EE_CHANNEL_RESOURCE, "cluster", newValue);
        } else {
            new Operations(client).undefineAttribute(JGROUPS_EE_CHANNEL_RESOURCE, "cluster");
        }
        return oldValue;
    }

}
