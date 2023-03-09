package org.wildfly.ejbclient.testsuite.integration.basic.async;

import jakarta.ejb.ApplicationException;

/**
 * @author Jan Martiska
 */
@ApplicationException(rollback = false)
public class AppException extends RuntimeException {
}
