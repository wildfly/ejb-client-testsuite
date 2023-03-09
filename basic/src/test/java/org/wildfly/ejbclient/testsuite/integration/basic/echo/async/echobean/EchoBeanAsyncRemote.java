package org.wildfly.ejbclient.testsuite.integration.basic.echo.async.echobean;

import java.util.concurrent.Future;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface EchoBeanAsyncRemote {

    default Future<Boolean> echo(Boolean value) {
        return new AsyncResult<>(value);
    }

    default Future<Character> echo(Character value) {
        return new AsyncResult<>(value);
    }

    default Future<Double> echo(Double value) {
        return new AsyncResult<>(value);
    }

    default Future<Float> echo(Float value) {
        return new AsyncResult<>(value);
    }

    default Future<Integer> echo(Integer value) {
        return new AsyncResult<>(value);
    }

    default Future<Long> echo(Long value) {
        return new AsyncResult<>(value);
    }

    default Future<Short> echo(Short value) {
        return new AsyncResult<>(value);
    }

}
