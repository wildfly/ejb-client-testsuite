package org.wildfly.ejbclient.testsuite.integration.basic.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static java.util.stream.Collectors.toList;

/**
 * Test for https://issues.redhat.com/browse/EJBCLIENT-159 and linked feature request
 * Resolves JUST jboss-ejb-client using the BOM and a dependency on jboss-ejb-client itself. Then it checks
 * whether all expected transitive dependencies are taken in.
 * @author Jan Martiska
 */
@RunWith(JUnit4.class)
@RunAsClient
//FIXME
@Ignore
public class MavenDependencyTestCase {

    private static Logger logger = Logger.getLogger(MavenDependencyTestCase.class.getName());

    /* Fake pom.xml file */
    private static File pom;
    private static String bomGroupId;
    private static String bomArtifactId;
    private static String bomVersion;
    private static String localRepositoryUrl;

    @BeforeClass
    public static void prepare() throws IOException {
        bomGroupId = System.getProperty("groupId.ejb.client.bom");
        if (bomGroupId == null) {
            throw new IllegalArgumentException(MavenDependencyTestCase.class.getSimpleName()
                  + " requires the groupId.ejb.client.bom to be set, but it is null");
        }
        bomArtifactId = System.getProperty("artifactId.ejb.client.bom");
        if (bomArtifactId == null) {
            throw new IllegalArgumentException(MavenDependencyTestCase.class.getSimpleName()
                    + " requires the artifactId.ejb.client.bom to be set, but it is null");
        }
        bomVersion = System.getProperty("version.ejb.client.bom");
        if (bomVersion == null) {
            throw new IllegalArgumentException(MavenDependencyTestCase.class.getSimpleName()
                  + " requires the version.ejb.client.bom to be set, but it is null");
        }
        localRepositoryUrl = Paths.get(System.getProperty("localRepository")).toRealPath().toUri().toURL().toString();
        if (localRepositoryUrl == null) {
            throw new IllegalArgumentException(MavenDependencyTestCase.class.getSimpleName()
                  + " requires the localRepository to be set, but it seems Surefire has changed its propagation");
        }

        pom = createPomXmlFile(bomGroupId, bomArtifactId, bomVersion, localRepositoryUrl);
    }

    @AfterClass
    public static void teardown() {
        if(pom.exists())
            pom.deleteOnExit();
    }

    @Test
    public void test() throws IOException {
        useProvidedSettingsXmlIfAny();
        final MavenResolvedArtifact[] actualArtifacts = Maven.configureResolver()
                .loadPomFromFile(pom)
                .resolve("org.jboss:jboss-ejb-client")
                .withTransitivity()
                .asResolvedArtifact();

        List<GroupAndArtifact> expectedArtifacts = new ArrayList<>();
        expectedArtifacts.add(new GroupAndArtifact("org.jboss.xnio", "xnio-nio"));
        expectedArtifacts.add(new GroupAndArtifact("org.jboss.xnio", "xnio-api"));
        expectedArtifacts.add(new GroupAndArtifact("org.jboss.remoting", "jboss-remoting"));
        expectedArtifacts.add(new GroupAndArtifact("org.jboss.marshalling", "jboss-marshalling"));
        expectedArtifacts.add(new GroupAndArtifact("org.jboss.marshalling", "jboss-marshalling-river"));
        expectedArtifacts.add(new GroupAndArtifact("org.jboss.logging", "jboss-logging"));

        // sasl
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-auth-server-sasl"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-digest"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-entity"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-external"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-gs2"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-gssapi"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-oauth2"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-otp"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-plain"));
        expectedArtifacts.add(new GroupAndArtifact("org.wildfly.security", "wildfly-elytron-sasl-scram"));


        // find the root jboss-ejb-client artifact itself
        final Optional<MavenResolvedArtifact> jbossEjbClientArtifact = Arrays.stream(actualArtifacts)
                .filter(it -> it.getCoordinate().getGroupId().equals("org.jboss") &&
                              it.getCoordinate().getArtifactId().equals("jboss-ejb-client"))
                .findAny();
        if (!jbossEjbClientArtifact.isPresent()) {
            Assert.fail("jboss-ejb-client dependency not resolved");
        }

        // prepare a list of resolved artifacts
        final List<GroupAndArtifact> actualArtifactsList = Arrays.stream(actualArtifacts)
                .filter(artifact -> artifact.getScope() == ScopeType.COMPILE) // take into account only compile-scoped artifacts
                .map(artifact -> new GroupAndArtifact(artifact.getCoordinate().getGroupId(),
                        artifact.getCoordinate().getArtifactId())
                ).collect(toList());

        expectedArtifacts.removeAll(actualArtifactsList);
        if(!expectedArtifacts.isEmpty()) {
            Assert.fail(
                    "Some required compile-scoped dependencies of org.jboss:jboss-ejb-client:" + jbossEjbClientArtifact.get()
                            .getResolvedVersion() + " using " + bomGroupId + ":wildfly-ejb-client-bom " + bomVersion
                            + " were not found:\n" + expectedArtifacts);
        }
    }

    private void useProvidedSettingsXmlIfAny() {
        // Hack to execute the resolver with the same settings.xml as provided to mvn command on cmdline (if present)
        // We need this otherwise we might not be able to resolve correct dependencies from testing maven repository
        // the resolver defaults to ~/.m2/settings.xml
        // According to docs we can leverage 'org.apache.maven.user-settings' system property to override the default settings.
        // https://github.com/shrinkwrap/resolver#system-properties
        // This hack currently relies on "sun.java.command" property being available on all tested JDKs; if this is no longer
        // true we might have to provide value for "org.apache.maven.user-settings" from commnadline ourselves.

        String mvnJavaCommand = System.getProperty("sun.java.command", "");
        if (mvnJavaCommand.contains(" -s ")) { //Maven was executed with provided settings.xml
            String providedSettingsPath = mvnJavaCommand.split(" -s ")[1].split(" ")[0];
            File providedSettingsFile = new File(providedSettingsPath);
            Assert.assertTrue("Parsing of provided settings.xml path failed, '" + providedSettingsPath + "' does not exist",
                    providedSettingsFile.exists() && providedSettingsFile.isFile());
            System.setProperty("org.apache.maven.user-settings", providedSettingsPath);
            logger.info("Using provided settings.xml to resolve artifacts: " + providedSettingsPath);
        }
    }

    /**
     * Creates a mock pom.xml file which only declares a dependency on the jboss-ejb-client and imports
     * versions using wildfly-ejb-client-bom.
     */
    public static File createPomXmlFile(String bomGroupId, String bomArtifactId, String bomVersion,
          String localRepositoryUrl) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n"
                + "\n"
                + "\n"
                + "    <groupId>org.jboss.ejb.test</groupId>\n"
                + "    <artifactId>ejb-client-dependency-test</artifactId>\n"
                + "    <version>1.0-SNAPSHOT</version>\n"
                + "    <packaging>jar</packaging>\n"
                + "\n"
                + "    <properties>\n"
                + "        <maven.compiler.target>11</maven.compiler.target>\n"
                + "        <maven.compiler.source>11</maven.compiler.source>\n"
                + "    </properties>\n"
                + "\n"
                + "    <dependencyManagement>\n"
                + "        <dependencies>\n"
                + "            <dependency>\n"
                + "                <groupId>" + bomGroupId + "</groupId>\n"
                + "                <artifactId>"+ bomArtifactId +"</artifactId>\n"
                + "                <version>"+ bomVersion +"</version>\n"
                + "                <type>pom</type>\n"
                + "                <scope>import</scope>\n"
                + "            </dependency>\n"
                + "        </dependencies>\n"
                + "    </dependencyManagement>\n"
                + "\n"
                + "    <dependencies>\n"
                + "        <dependency>\n"
                + "            <groupId>org.jboss</groupId>\n"
                + "            <artifactId>jboss-ejb-client</artifactId>\n"
                + "        </dependency>\n"
                + "    </dependencies>\n"
                + "\n"
                + "    <repositories>\n"
                + "        <repository>\n"
                + "            <id>testsuite-local-repo</id>\n"
                + "            <name>Local repository of the testsuite</name>\n"
                + "            <url>\n"
                + "                " + localRepositoryUrl + "\n"
                + "            </url>\n"
                + "        </repository>\n"
                + "    </repositories>\n"
                + "\n"
                + "</project>\n";
        final Path file = Files.createTempFile("mock-pom", "xml");
        Files.write(file, content.getBytes());
        return file.toFile();
    }

    static class GroupAndArtifact {
        private final String groupId;
        private final String artifactId;

        public GroupAndArtifact(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GroupAndArtifact that = (GroupAndArtifact)o;

            if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) {
                return false;
            }
            return artifactId != null ? artifactId.equals(that.artifactId) : that.artifactId == null;

        }

        @Override
        public int hashCode() {
            int result = groupId != null ? groupId.hashCode() : 0;
            result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "[" + groupId + ":" + artifactId + "]";
        }
    }
}
