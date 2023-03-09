package org.wildfly.ejbclient.testsuite.integration.basic.async;

import java.util.concurrent.Future;
import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface LongRunningAsyncRemote {

    Future<Integer> computeAnswerToEverything() throws InterruptedException;

    boolean isCancelCalled();

}
