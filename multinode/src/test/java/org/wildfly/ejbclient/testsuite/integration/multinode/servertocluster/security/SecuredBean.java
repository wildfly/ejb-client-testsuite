package org.wildfly.ejbclient.testsuite.integration.multinode.servertocluster.security;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@SecurityDomain("other")
@Stateless
public class SecuredBean implements SecuredBeanRemote {

	@Resource
	private SessionContext ctx;

	@RolesAllowed("users")
	public String whoAmI() {
		return ctx.getCallerPrincipal().getName() + "@" + System.getProperty("jboss.node.name");
	}

}
