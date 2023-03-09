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
