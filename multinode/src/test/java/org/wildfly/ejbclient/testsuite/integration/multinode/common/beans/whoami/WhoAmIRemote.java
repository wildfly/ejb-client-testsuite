package org.wildfly.ejbclient.testsuite.integration.multinode.common.beans.whoami;

import jakarta.ejb.Remote;

@Remote
public interface WhoAmIRemote {

	String whoAmI();

}
