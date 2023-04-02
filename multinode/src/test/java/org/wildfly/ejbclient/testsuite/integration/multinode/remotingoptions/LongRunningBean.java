package org.wildfly.ejbclient.testsuite.integration.multinode.remotingoptions;

import jakarta.ejb.Remote;

@Remote
public interface LongRunningBean {

	/**
	 * Waits for a specified number of seconds and then returns.
	 */
	void doWork(int seconds);

}
