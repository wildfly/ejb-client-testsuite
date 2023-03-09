package org.wildfly.ejbclient.testsuite.integration.basic.beanpool;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.Pool;
import org.jboss.logging.Logger;

@Stateless
@Pool("size1poolTimeout")
public class LongProcessingBeanStateless2 implements LongProcessingBeanRemote {

	// this is used to make sure that different instances of this bean will have different hashCode
	private Long randomNumber = ThreadLocalRandom.current().nextLong();

	private static Logger logger = Logger.getLogger(LongProcessingBeanStateless2.class.getName());

	@Override
	public int processAndReturnInstanceHashCode(Integer seconds) {
		try {
			logger.info("Processing will now sleep for " + seconds + " seconds, instance hashCode= " + hashCode());
			TimeUnit.SECONDS.sleep(seconds);
			logger.info("Processing now done");
			return hashCode();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		LongProcessingBeanStateless2 that = (LongProcessingBeanStateless2)o;

		return randomNumber != null ? randomNumber.equals(that.randomNumber) : that.randomNumber == null;
	}

	@Override
	public int hashCode() {
		return randomNumber != null ? randomNumber.hashCode() : 0;
	}
}
