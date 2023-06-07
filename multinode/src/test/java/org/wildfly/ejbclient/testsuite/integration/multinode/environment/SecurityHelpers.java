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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;
import org.wildfly.extras.creaper.core.CommandFailedException;

public class SecurityHelpers {

    private static Logger logger = Logger.getLogger(SecurityHelpers.class.getName());

    public static void createTestingUsers(String jbossHome)
            throws IOException, CommandFailedException {
        Files.copy(
                Paths.get(ClassLoader.getSystemResource("org/wildfly/ejbclient/testsuite/integration/multinode/environment/application-users.properties").getFile()),
                Paths.get(jbossHome, "standalone", "configuration", "application-users.properties"),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
                Paths.get(ClassLoader.getSystemResource("org/wildfly/ejbclient/testsuite/integration/multinode/environment/application-roles.properties").getFile()),
                Paths.get(jbossHome, "standalone", "configuration", "application-roles.properties"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    public static void removeTestingUsers(String jbossHome)
            throws IOException, CommandFailedException {
        Files.copy(
                Paths.get(ClassLoader.getSystemResource("org/wildfly/ejbclient/testsuite/integration/multinode/environment/application-users-empty.properties").getFile()),
                Paths.get(jbossHome, "standalone", "configuration", "application-users.properties"),
                StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
                Paths.get(ClassLoader.getSystemResource("org/wildfly/ejbclient/testsuite/integration/multinode/environment/application-roles-empty.properties").getFile()),
                Paths.get(jbossHome, "standalone", "configuration", "application-roles.properties"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

}
