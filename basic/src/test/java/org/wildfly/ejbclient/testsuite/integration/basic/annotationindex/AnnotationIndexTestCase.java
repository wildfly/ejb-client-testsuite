package org.wildfly.ejbclient.testsuite.integration.basic.annotationindex;

import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.SecurityManagerUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import ejbjar.DummyEJB;
import ejbjar.DummyEJBRemote;

/**
 * Test for automatic index generation on static modules.
 * See https://issues.redhat.com/browse/WFCORE-493
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
public class AnnotationIndexTestCase {

    public static final String ARCHIVE_NAME = "annotation-index-test";

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        archive.addAsManifestResource(
                new ByteArrayAsset("Dependencies: my-awesome-ejb-module annotations, jakarta.ejb.api annotations, org.jboss.weld.core, org.jboss.weld.spi\n".getBytes()),
                "MANIFEST.MF");
        /*
         * These permissions are needed for the test tryInvokeEJBFromContainer,
         * all just because of Arquillian when running an in-container test.
         */
        archive.addAsManifestResource(
                SecurityManagerUtils.createPermissionsXmlAsset(
                        new ReflectPermission("suppressAccessChecks"),
                        new RuntimePermission("accessDeclaredMembers"),
                        new PropertyPermission("arquillian.debug", "read")
                ),
                "permissions.xml");
        return archive;
    }

    @Test
    @RunAsClient
    public void tryInvokeEJBFromClient() throws Exception {
        final InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get();
        final DummyEJBRemote ejb = ctx
                .lookupStateless(ARCHIVE_NAME, DummyEJB.class, DummyEJBRemote.class);
        ejb.sayAJoke();
    }

    @Test
    public void tryInvokeEJBFromContainer() throws Exception {
        final DummyEJBRemote ejb1 = (DummyEJBRemote)new InitialContext().lookup("java:module/DummyEJB!ejbjar.DummyEJBRemote");
        ejb1.sayAJoke();
    }
}
