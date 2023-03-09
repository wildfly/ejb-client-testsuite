# EJB client QA test suite

## How to run TS with bom file
`mvn test $SYSTEM_PROPERTIES` for `$SYSTEM_PROPERTIES` see below:


| PROPERTY                  |REQUIRED?  |DESCRIPTION                                                             |
|---------------------------|-----------|------------------------------------------------------------------------|
| server.home               |true       |Path to server directory                                                |
|                           |           |
| authentication.type       |false      |Authentication mechanism for client-server communication
|                           |           |Allowed values: `user`, `local`
|                           |           |Default value: `user` (that means standard user+password auth)
|                           |           |`local` currently not supported for https connections
|                           |           |   (this scenario doesn't make much sense anyway)
|                           |           |
| context.type              |false      |Type of EJB client context to use.
|                           |           |Allowed values: `global`, `scoped`, `wildfly-naming-client`
|                           |           |
| connector                 |false      |Which server connector should be used for EJB invocations
|                           |           |Allowed values: `remoting`, `http-remoting`, 
|                           |           |`https-remoting`, `http`, `remoting-ssl`
|                           |           |Default value: `http-remoting`
|                           |           |
| groupId.ee.bom            |false      |Group ID of server BOM
|                           |           |
| artifactId.ee.bom         |false      |Artifact ID of server BOM
|                           |           |
| version.ee.bom            |false      |Version ID of server BOM
|                           |           |
| groupId.ejb.client.bom    |false      |Group ID of the BOM for ejb client
|                           |           |
| artifactId.ejb.client.bom |false      |Artifact ID of the BOM for ejb client
|                           |           |
| version.ejb.client.bom    |false      |Version of the BOM for ejb client
|                           |           |
| debugServer               |false      |If this property is present, it will enable JPDA debugging
|                           |           |of the server on port 8787. Note that this really debugs
|                           |           |the *server* JVM. If you need to debug the test's (client's) JVM,
|                           |           |use `-Dmaven.surefire.debug`
|                           |           |
| legacy                    |false      |If specified, the TS will use the "legacy EJB client".
|                           |           |See the details bellow.

Legacy bom file was introduced in [WFLY-8707](https://issues.redhat.com/browse/WFLY-8707).
Generally there are two versions of EJB client those are not source-compatible
and some classes in this TS are compilable only with EJB client 3.x (legacy client) while some others only with 5.x.
Activating the legacy profile tells Maven to skip compilation of classes which are not compilable with the
requested EJB client version.

### Examples how to run the TS

```
mvn clean -Dserver.home=/tmp/wildfly-27.0.1.Final test -Dtest=WrongProtocolLoggingTestCase
```

```
mvn clean -Dconnector=http-remoting -Dcontext.type=wildfly-naming-client -Dauthentication.type=user \
  -Dserver.home=/tmp/wildfly-27.0.1.Final test -Dtest=AnnotationIndexTestCase
```

## How to run TS with jboss-client.jar from server distribution

### Prepare dependency

Deploy jboss-client.jar from server distribution as dependency to local maven repo

```
mvn -f pom-wildfly-client.xml clean install -Dprepare -Dserver.zip.url=url/to/zip/distribution/of/server.zip
```

Examples:
```
mvn -f pom-wildfly-client.xml clean install -Dprepare -Dserver.zip.url=file:///path/to/wildfly-27.0.1.Final.zip
mvn -f pom-wildfly-client.xml clean install -Dprepare -Dserver.zip.url=https://github.com/wildfly/wildfly/releases/download/27.0.1.Final/wildfly-27.0.1.Final.zip
```

### Start the TS

`mvn -f pom-wildfly-client.xml test $SYSTEM_PROPERTIES` for `authentication.type`, `context.type`, `connector` and `debugServer` parameters
described in "How to run TS with bom file" chapter of this file.

Example:

```
mvn -f pom-wildfly-client.xml test -Dtest=WrongProtocolLoggingTestCase
```

## Testing with Security Manager enabled
Security manager is enabled by default on the client side, with a permit-all policy file (`src/test/resources/permit.policy`).
On the server side, activate the security manager using `-Psecurity.manager.server`

## JaCoCo
To generate a JaCoCo exec files, run the ts with `-Pjacoco` profile.
JaCoCo exec files will be generated: 
- from the client side: `target/jacoco-client.exec`
- from the server side: `target/jacoco-server.exec`
- client+server side merged together: `target/jacoco-merged.exec`

## Some technical details, no need to read this if you just want to run the TS:
### Server setup
Server setup tasks are done in the `process-test-resources` phase.
Server is started using `wildfly-maven-plugin`, then some setup CLI scripts are executed, then server is stopped.
CLI scripts are in the `setup` directory.

### Server resources needed for tests
Resources which are used by the server during testing are in the `files` directory.
They are copied to their destinations in server distribution using Ant tasks during `process-test-resources` phase.

### Byteman usage
Some tests use Byteman. Currently only on the CLIENT side. Byteman jar and script are provided in the `byteman` directory.
Byteman is added to the client side tests using the surefire plugin's argLine.
