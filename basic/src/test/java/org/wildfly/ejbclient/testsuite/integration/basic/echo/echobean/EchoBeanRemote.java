package org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean;

import jakarta.ejb.Remote;

import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author Jan Martiska
 */
@Remote
public interface EchoBeanRemote {

    @CompressionHint(compressResponse = true, compressRequest = true)
    default boolean echo(boolean value) {
        return value;
    }

    default Boolean echo(Boolean value) {
        return value;
    }

    default char echo(char value) {
        return value;
    }

    default Character echo(Character value) {
        return value;
    }

    default double echo(double value) {
        return value;
    }

    default Double echo(Double value) {
        return value;
    }

    default float echo(float value) {
        return value;
    }

    default Float echo(Float value) {
        return value;
    }

    default int echo(int value) {
        return value;
    }

    default Integer echo(Integer value) {
        return value;
    }

    default long echo(long value) {
        return value;
    }

    default Long echo(Long value) {
        return value;
    }

    default short echo(short value) {
        return value;
    }

    default Short echo(Short value) {
        return value;
    }

    default String echo(String value) { return value; }

}
