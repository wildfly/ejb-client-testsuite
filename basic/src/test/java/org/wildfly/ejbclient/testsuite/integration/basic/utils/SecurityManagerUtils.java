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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.Permission;

import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

public class SecurityManagerUtils {

    public static Asset createPermissionsXmlAsset(Permission... permissions) {
        final Element permissionsElement = new Element("permissions");
        permissionsElement.setNamespaceURI("http://xmlns.jcp.org/xml/ns/javaee");
        permissionsElement.addAttribute(new Attribute("version", "7"));
        for (Permission permission : permissions) {
            final Element permissionElement = new Element("permission");

            final Element classNameElement = new Element("class-name");
            final Element nameElement = new Element("name");
            classNameElement.appendChild(permission.getClass().getName());
            nameElement.appendChild(permission.getName());
            permissionElement.appendChild(classNameElement);
            permissionElement.appendChild(nameElement);

            final String actions = permission.getActions();
            if (actions != null && ! actions.isEmpty()) {
                final Element actionsElement = new Element("actions");
                actionsElement.appendChild(actions);
                permissionElement.appendChild(actionsElement);
            }
            permissionsElement.appendChild(permissionElement);
        }
        Document document = new Document(permissionsElement);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            final NiceSerializer serializer = new NiceSerializer(stream);
            serializer.setIndent(4);
            serializer.setLineSeparator("\n");
            serializer.write(document);
            serializer.flush();
            return new StringAsset(stream.toString("UTF-8"));
        } catch (IOException e) {
            throw new IllegalStateException("Generating permissions.xml failed", e);
        }
    }

    static class NiceSerializer extends Serializer {

        public NiceSerializer(OutputStream out) throws UnsupportedEncodingException {
            super(out, "UTF-8");
        }

        protected void writeXMLDeclaration() throws IOException {
            super.writeXMLDeclaration();
            super.breakLine();
        }
    }



}
