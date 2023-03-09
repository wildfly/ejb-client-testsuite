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
