package org.wildfly.ejbclient.testsuite.integration.basic.security;

import java.util.concurrent.Future;
import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface WhoAmIRemote {

    String whoAmI();

    Future<String> whoAmIAsync();

    boolean amIInRole(String role);

}
