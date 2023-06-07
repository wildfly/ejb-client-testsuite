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

package org.wildfly.ejbclient.testsuite.integration.basic.security;

import java.util.concurrent.Future;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author Jan Martiska
 */
@Stateful
@SecurityDomain(value = "other2", unauthenticatedPrincipal = WhoAmIBeanStateful.UNAUTHENTICATED_PRINCIPAL)
public class WhoAmIBeanStateful implements WhoAmIRemote {


    public static final String UNAUTHENTICATED_PRINCIPAL = "someone";

    @Resource
    private SessionContext ctx;

    @Override
    @RolesAllowed("**")
    public String whoAmI() {
        return ctx.getCallerPrincipal().getName();
    }

    @Override
    @Asynchronous
    @RolesAllowed("**")
    public Future<String> whoAmIAsync() {
        return new AsyncResult<>(ctx.getCallerPrincipal().getName());
    }

    @Override
    @RolesAllowed("**")
    public boolean amIInRole(String role) {
        return ctx.isCallerInRole(role);

    }
}
