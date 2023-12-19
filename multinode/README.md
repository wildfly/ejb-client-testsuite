# EJB multi server test suite

## Description
The goal of this TS is to cover various complex scenarios involving EJB client and multiple servers:

- client-to-cluster scenarios
- server-to-server scenarios
- cluster-to-cluster scenarios

etc.

## How to run
To run this, you need to specify the path to the server distribution you want to run the test against.
Also you should specify a particular server bom version to mark the correct 
EJB client library version you want to use in the test.

Example:

`mvn test -DspecificModule=multinode -Dserver.zip=/PATH/TO/SERVER.ZIP`

The first time you run this, it will create a fresh unzip for each EAP node (see section "Nodes").
Unless you run `mvn clean`, subsequent `mvn test` invocations will skip the unzipping process to save a bit of 
time because the nodes will be there unzipped already (you need to make sure that they are left with a reasonable configuration).

You can also define bom files which should be used by those system properties:
`groupId.ee.bom`, `artifactId.ee.bom`, `version.ee.bom`, `groupId.ejb.client.bom`, `artifactId.ejb.client.bom`, `version.ejb.client.bom`.
These properties are described in [readme file for basic client tests](../basic).

## How to run TS with jboss-client.jar from server distribution

### Prepare dependency

Deploy jboss-client.jar from server distribution as dependency to local maven repo

```
cd multinode
mvn -f pom-wildfly-client.xml clean install -Dprepare -Dserver.zip.url=url/to/zip/distribution/of/server.zip
```

Examples:
```
cd multinode
mvn -f pom-wildfly-client.xml clean install -Dprepare -Dserver.zip.url=file:///path/to/wildfly-28.0.0.Beta1.zip
mvn -f pom-wildfly-client.xml clean install -Dprepare -Dserver.zip.url=https://github.com/wildfly/wildfly/releases/download/30.0.1.Final/wildfly-30.0.1.Final.zip
```

### Start the TS

`mvn -f pom-wildfly-client.xml test`

Example:

```
mvn -f pom-wildfly-client.xml test -Dtest=ClusterNodeSelectorTestCase
```

## IPv6
Run the TS in IPv6 mode by activating the profile `-Pipv6`.
However, for this to work, you most probably need to enable multicast on your loopback address.
This can be done by

``
sudo ip -6 route add table local local ff05::/16 dev lo metric 5
``

The mask (`ff05::/16` in this example) has to cover the addresses `ff05:1000::100:11` and `ff05:1000::100:12`, which are the default multicast addresses for clusters created by the test suite.
 
## Nodes
The TS creates a separate EAP distribution (unzip) for each node that will be tested. List of nodes:

|Node name|Profile|Port offset|
|---|---|---|
|node1|standalone.xml|0|
|node2|standalone.xml|100|
|cluster1-node1|standalone-ha.xml|200|
|cluster1-node2|standalone-ha.xml|300|
|cluster2-node1|standalone-ha.xml|400|
|cluster2-node2|standalone-ha.xml|500|

### Clusters
If running on Jenkins, don't forget to change the multicast addresses 
appropriately so your clusters don't clash with other clusters on different machines!

|Cluster name|Default IPv4 multicast address|Default IPv6 multicast address|Multicast address property|
|---|---|---|---|
|cluster1|`230.0.0.100`|`ff05:1000::100:11`|`-Dcluster1.multicast-address`|
|cluster2|`230.0.0.200`|`ff05:1000::100:12`|`-Dcluster2.multicast-address`|


## JaCoCo
To have JaCoCo reports generated, run the ts with `-Pjacoco`.
Jacoco coverage file will be generated for each node separately: `target/jacoco-*.exec`
Merged exec file will be saved as `target/jacoco-merged.exec`
Aggregated HTML report will be generated: `target/jacoco/index.html`
