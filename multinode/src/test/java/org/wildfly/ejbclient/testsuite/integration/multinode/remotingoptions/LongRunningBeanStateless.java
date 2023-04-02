package org.wildfly.ejbclient.testsuite.integration.multinode.remotingoptions;

import java.util.concurrent.TimeUnit;
import jakarta.ejb.Stateless;

@Stateless
public class LongRunningBeanStateless implements LongRunningBean {

	@Override
	public void doWork(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
