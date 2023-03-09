package org.wildfly.ejbclient.testsuite.integration.basic.beanpool;

import jakarta.ejb.Remote;

@Remote
public interface LongProcessingBeanRemote {

	int processAndReturnInstanceHashCode(Integer seconds);

}
