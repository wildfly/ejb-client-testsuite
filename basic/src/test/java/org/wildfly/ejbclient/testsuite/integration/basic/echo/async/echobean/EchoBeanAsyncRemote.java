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
