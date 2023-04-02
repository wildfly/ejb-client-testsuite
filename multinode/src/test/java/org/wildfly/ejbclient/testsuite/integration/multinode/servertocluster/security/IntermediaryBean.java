package org.wildfly.ejbclient.testsuite.integration.multinode.servertocluster.security;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class IntermediaryBean implements IntermediaryBeanRemote {

	@EJB(lookup = "ejb:/secured/SecuredBean!org.wildfly.ejbclient.testsuite.integration.multinode.servertocluster.security.SecuredBeanRemote")
	SecuredBeanRemote securedBean;

	@Override
	public String callRemoteSecuredBean() {
		return securedBean.whoAmI();
	}

}
