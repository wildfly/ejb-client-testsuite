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

    default String echo(String value) {
        return value;
    }

}
