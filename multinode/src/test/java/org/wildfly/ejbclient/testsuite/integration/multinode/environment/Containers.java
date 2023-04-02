package org.wildfly.ejbclient.testsuite.integration.multinode.environment;

import java.net.UnknownHostException;

public class Containers {

    public static final Container NODE1;
    public static final Container NODE2;
    public static final Container CLUSTER1_NODE1;
    public static final Container CLUSTER1_NODE2;
    public static final Container CLUSTER2_NODE1;
    public static final Container CLUSTER2_NODE2;

    static {
        try {
            NODE1 = new Container("node1",
                    System.getProperty("node1.address"),
                    Integer.parseInt(System.getProperty("node1.application-port")),
                    Integer.parseInt(System.getProperty("node1.management-port")),
                    System.getProperty("node1.jbossHome"),
                    "standalone.xml");
            NODE2 = new Container("node2",
                    System.getProperty("node2.address"),
                    Integer.parseInt(System.getProperty("node2.application-port")),
                    Integer.parseInt(System.getProperty("node2.management-port")),
                    System.getProperty("node2.jbossHome"),
                    "standalone.xml");
            CLUSTER1_NODE1 = new Container("cluster1-node1",
                    System.getProperty("cluster1-node1.address"),
                    Integer.parseInt(System.getProperty("cluster1-node1.application-port")),
                    Integer.parseInt(System.getProperty("cluster1-node1.management-port")),
                    System.getProperty("cluster1-node1.jbossHome"),
                    "standalone-ha.xml");
            CLUSTER1_NODE2 = new Container("cluster1-node2",
                    System.getProperty("cluster1-node2.address"),
                    Integer.parseInt(System.getProperty("cluster1-node2.application-port")),
                    Integer.parseInt(System.getProperty("cluster1-node2.management-port")),
                    System.getProperty("cluster1-node2.jbossHome"),
                    "standalone-ha.xml");
            CLUSTER2_NODE1 = new Container("cluster2-node1",
                    System.getProperty("cluster2-node1.address"),
                    Integer.parseInt(System.getProperty("cluster2-node1.application-port")),
                    Integer.parseInt(System.getProperty("cluster2-node1.management-port")),
                    System.getProperty("cluster2-node1.jbossHome"),
                    "standalone-ha.xml");
            CLUSTER2_NODE2 = new Container("cluster2-node2",
                    System.getProperty("cluster2-node2.address"),
                    Integer.parseInt(System.getProperty("cluster2-node2.application-port")),
                    Integer.parseInt(System.getProperty("cluster2-node2.management-port")),
                    System.getProperty("cluster2-node2.jbossHome"),
                    "standalone-ha.xml");
        } catch (Exception e) {
            e.printStackTrace(); // log it so we can see it in the output
            throw new RuntimeException(e);
        }
    }

    public static class Container {

        public final String nodeName;
        public final String bindAddress;
        public final Integer applicationPort;
        public final Integer managementPort;
        public final String homeDirectory;
        public final String urlOfHttpRemotingConnector;
        public final String configurationXmlFile;

        Container(String nodeName, String bindAddress, Integer applicationPort, Integer managementPort,
                  String homeDirectory, String configuration) throws UnknownHostException {
            this.nodeName = nodeName;
            this.bindAddress = bindAddress;
            this.applicationPort = applicationPort;
            this.managementPort = managementPort;
            this.homeDirectory = homeDirectory;
            this.urlOfHttpRemotingConnector = "remote+http://" + MiscHelpers
                    .formatPossibleIPv6Address(bindAddress) + ":" + applicationPort;
            this.configurationXmlFile = configuration;
        }

        @Override
        public String toString() {
            return nodeName;
        }
    }

}
