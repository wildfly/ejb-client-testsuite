package org.wildfly.ejbclient.testsuite.integration.basic.lambda;

import java.util.function.Supplier;
import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
public class LambdaBean implements LambdaRemote<String> {

    @Override
    public String evaluateAndReturn(Supplier<String> supplier) {
        return supplier.get();
    }

}
