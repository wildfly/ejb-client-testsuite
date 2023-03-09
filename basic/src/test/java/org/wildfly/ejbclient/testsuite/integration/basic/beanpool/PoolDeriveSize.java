package org.wildfly.ejbclient.testsuite.integration.basic.beanpool;

public enum PoolDeriveSize {
	NONE("none"),
	FROM_WORKER_POOLS("from-worker-pools"),
	FROM_CPU_COUNT("from-cpu-count");

	private String s;

	PoolDeriveSize(String s) {
		this.s = s;
	}

	@Override
	public String toString() {
		return s;
	}
}
