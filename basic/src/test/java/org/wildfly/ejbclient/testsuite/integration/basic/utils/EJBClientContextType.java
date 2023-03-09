package org.wildfly.ejbclient.testsuite.integration.basic.utils;

/**
 * @author Jan Martiska
 */
public enum EJBClientContextType {

    WILDFLY_NAMING_CLIENT ("wildfly-naming-client"), // using the wildfly-naming-client project
    GLOBAL ("global"),
    SCOPED ("scoped");

    private final String type;

    EJBClientContextType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static EJBClientContextType getValue(String requestedValue) {
        for (EJBClientContextType ct : EJBClientContextType.values()) {
            if (ct.toString().equalsIgnoreCase(requestedValue)) {
                return ct;
            }
        }
        throw new IllegalArgumentException("Unknown ejb client context type: " + requestedValue);
    }
}
