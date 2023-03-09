package org.wildfly.ejbclient.testsuite.integration.basic.lambda;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * @author Jan Martiska
 */
public class HelperBean {

    public static String invoke(LambdaRemote<String> bean) {
        return bean.evaluateAndReturn((Serializable & Supplier<String>)() -> "99");
    }
}
