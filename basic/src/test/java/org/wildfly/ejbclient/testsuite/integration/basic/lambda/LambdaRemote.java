package org.wildfly.ejbclient.testsuite.integration.basic.lambda;

import java.util.function.Supplier;
import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface LambdaRemote<T> {

    T evaluateAndReturn(Supplier<T> supplier);
}
