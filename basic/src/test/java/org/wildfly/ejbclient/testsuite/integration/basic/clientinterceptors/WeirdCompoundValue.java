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

package org.wildfly.ejbclient.testsuite.integration.basic.clientinterceptors;

import java.io.Serializable;

/**
 * @author Jan Martiska
 */
public class WeirdCompoundValue implements Serializable {

    private final int integer;
    private final String string;

    public WeirdCompoundValue() {
        this.integer = (int)Math.round(Math.random() * Integer.MAX_VALUE);
        this.string = "Hahaha";
    }


    public int getInteger() {
        return integer;
    }

    public String getString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeirdCompoundValue)) {
            return false;
        }

        WeirdCompoundValue that = (WeirdCompoundValue)o;

        return integer == that.integer && string.equals(that.string);

    }

    @Override
    public int hashCode() {
        int result = integer;
        result = 31 * result + string.hashCode();
        return result;
    }
}
