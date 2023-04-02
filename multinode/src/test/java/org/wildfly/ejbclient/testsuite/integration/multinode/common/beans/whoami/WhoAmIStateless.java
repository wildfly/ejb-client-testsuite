package org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whoami;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

@Stateless
@PermitAll
public class WhoAmIStateless implements WhoAmIRemote {

	@Resource
	private SessionContext ctx;

	@Override
	@RolesAllowed("users")
	public String whoAmI() {
		return ctx.getCallerPrincipal().getName();
	}

}
