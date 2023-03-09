package org.wildfly.ejbclient.testsuite.integration.basic.echo.async.echobean;

import java.util.concurrent.Future;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateful;

/**
 * @author Jan Martiska
 */
@Stateful
@Asynchronous
public class EchoBeanAsyncStateful implements EchoBeanAsyncRemote {

    @Override
    public Future<Double> echo(Double value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

    @Override
    public Future<Boolean> echo(Boolean value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

    @Override
    public Future<Character> echo(Character value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

    @Override
    public Future<Float> echo(Float value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

    @Override
    public Future<Integer> echo(Integer value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

    @Override
    public Future<Long> echo(Long value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

    @Override
    public Future<Short> echo(Short value) {
        return EchoBeanAsyncRemote.super.echo(value);
    }

}
