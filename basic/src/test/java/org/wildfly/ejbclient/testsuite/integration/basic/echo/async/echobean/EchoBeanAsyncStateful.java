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
