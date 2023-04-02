package org.wildfly.ejbclient.testsuite.integration.multinode.environment;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class MiscHelpers {

    public static String generateRandomString() {
        return UUID.randomUUID().toString().substring(1, 16);
    }

    /**
     * Returns true if the TS is running in IPv6 mode, false otherwise.
     */
    public static boolean isIPv6() {
        return System.getProperty("ipv6", "false").equalsIgnoreCase("true");
    }

    /**
     * If the passed address is an IPv6 address, enclose it in brackets so it can be properly used as part of URLs.
     * It it is an IPv4 address, just return the same address string.
     */
    public static String formatPossibleIPv6Address(String address) throws UnknownHostException {
        final Class<? extends InetAddress> clazz = InetAddress.getByName(address).getClass();
        if (clazz == Inet6Address.class) {
            return "[" + address + "]";
        } else {
            return address;
        }
    }

    public static void safeCloseEjbClientContext(InitialContext ctx) {
        try {
            Context ejbContext = (Context)ctx.lookup("ejb:");
            if ( ejbContext != null ) {
                ejbContext.close();
            }
        } catch (NamingException e) {
            e.printStackTrace();
        }
        try {
            ctx.close();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

}
