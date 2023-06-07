/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.ejbclient.testsuite.integration.basic.beanpool;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.Pool;
import org.jboss.logging.Logger;

@Stateless
@Pool("size1pool")
public class LongProcessingBeanStateless implements LongProcessingBeanRemote {

	// this is used to make sure that different instances of this bean will have different hashCode
	private Long randomNumber = ThreadLocalRandom.current().nextLong();

	private static Logger logger = Logger.getLogger(LongProcessingBeanStateless.class.getName());

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

		LongProcessingBeanStateless that = (LongProcessingBeanStateless)o;

		return randomNumber != null ? randomNumber.equals(that.randomNumber) : that.randomNumber == null;
	}

	@Override
	public int hashCode() {
		return randomNumber != null ? randomNumber.hashCode() : 0;
	}
}
