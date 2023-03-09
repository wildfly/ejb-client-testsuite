package org.wildfly.ejbclient.testsuite.integration.basic.addressbinding;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.client.EJBClient;
import org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.client.EJBClientRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBean;
import org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ModelUtil;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.management.ManagementOperations;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * @author Ivan Straka
 * https://issues.redhat.com/browse/WFCORE-2578
 */

@RunWith(Arquillian.class)
@RunAsClient
public class SetSourceIpAddressTestCase_EJBCLIENT4 {

    public static final String SERVER_ARCHIVE_NAME = "source-ip-address-server";
    public static final String CLIENT_ARCHIVE_NAME = "source-ip-address-client";

    @BeforeClass
    public static void prerequisities() throws IOException {
        // doesn't work for legacy client
        Assume.assumeFalse(TestEnvironment.isLegacyEjbClient());
    }

    @Deployment(name = SERVER_ARCHIVE_NAME, testable = false, order = 1)
    public static JavaArchive serverDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, SERVER_ARCHIVE_NAME + ".jar");
        archive.addPackage(IpSourceAddressReturningBean.class.getPackage());
        return archive;
    }

    @Deployment(name = CLIENT_ARCHIVE_NAME, testable = false, order = 2)
    public static JavaArchive clientDeploymentWithWildflyConfig() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CLIENT_ARCHIVE_NAME + ".jar");
        archive.addClasses(EJBClient.class, EJBClientRemote.class, IpSourceAddressReturningBeanRemote.class);
        archive.addAsManifestResource("addressbinding/jboss-ejb-client.xml",  "jboss-ejb-client.xml");
        return archive;
    }

//    @Test
//    @InSequence(1)
//    public void testHostAndPortBindingStandaloneClient() throws Exception {
//        System.setProperty("wildfly.config.url", Thread.currentThread().getContextClassLoader().getResource("addressbinding/wildfly-config-port.xml").getFile());
//        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
//            IpSourceAddressReturningBeanRemote addressReturningBean = ctx.lookupStateful(SERVER_ARCHIVE_NAME, IpSourceAddressReturningBean.class, IpSourceAddressReturningBeanRemote.class);
//            String sourceAddress = addressReturningBean.getSourceAddress();
//            System.out.println("---- client address :: " + sourceAddress);
//            Assert.assertTrue("client should be bound to 127.0.0.2:12345", sourceAddress.contains("127.0.0.2:12345"));
//        }
//
//    }

    @Test
    @InSequence(2)
    public void testHostBindingStandaloneClient() throws Exception {
        Assume.assumeFalse("https://issues.redhat.com/browse/WEJBHTTP-108", TestEnvironment.getConnectorType() == ConnectorType.HTTP || TestEnvironment.getConnectorType() == ConnectorType.HTTPS);
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            IpSourceAddressReturningBeanRemote addressReturningBean = ctx.lookupStateful(SERVER_ARCHIVE_NAME, IpSourceAddressReturningBean.class, IpSourceAddressReturningBeanRemote.class);
            String sourceAddress = addressReturningBean.getSourceAddress();
            System.out.println("---- client address :: " + sourceAddress);
            Assert.assertTrue("client should be bound to 127.0.0.3 but is: " + sourceAddress, sourceAddress.contains("127.0.0.3"));
        }

    }

    @Test
    @OperateOnDeployment(CLIENT_ARCHIVE_NAME)
    @InSequence(5)
    public void addOutboundBindAddress(@ArquillianResource ManagementClient managementClient) throws IOException, InterruptedException {
        ModelNode op = ModelUtil.createOpNode("subsystem=io/worker=default/outbound-bind-address=default", "add");

        op.get("bind-address").set("127.0.0.5");
        op.get("bind-port").set(12354);
        op.get("match").set("0.0.0.0/0");
        ModelUtil.execute(managementClient, op);

        ManagementOperations.reloadServer();
    }

    @Test
    @InSequence(10)
    public void testHostAndPortBindingInContainer() throws Exception {
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            EJBClientRemote ejbClientRemote = ctx.lookupStateful(CLIENT_ARCHIVE_NAME, EJBClient.class, EJBClientRemote.class);
            String sourceAddress = ejbClientRemote.connectToRemoteBeanAndGetMyIp();
            System.out.println("---- client address :: " + sourceAddress);
            Assert.assertTrue("client should be bound to 127.0.0.5:12354, but is: " + sourceAddress, sourceAddress.contains("127.0.0.5:12354"));
        }
    }

    @Test
    @OperateOnDeployment(CLIENT_ARCHIVE_NAME)
    @InSequence(15)
    public void changeOutboundBindAddress(@ArquillianResource ManagementClient managementClient) throws IOException, InterruptedException {
        ModelNode changeBindAddress = ModelUtil.createOpNode("subsystem=io/worker=default/outbound-bind-address=default", "write-attribute");
        changeBindAddress.get("name").set("bind-address");
        changeBindAddress.get("value").set("127.0.0.10");

        ModelNode changeBindPort = ModelUtil.createOpNode("subsystem=io/worker=default/outbound-bind-address=default", "write-attribute");
        changeBindPort.get("name").set("bind-port");
        changeBindPort.get("value").set(0);

        ModelUtil.execute(managementClient, ModelUtil.createCompositeNode(changeBindAddress, changeBindPort));

        ManagementOperations.reloadServer();
    }

    @Test
    @InSequence(20)
    public void testHostBindingInContainer() throws Exception {
        try (InitialContextDirectory ctx = new InitialContextDirectory.Supplier().get()) {
            EJBClientRemote ejbClientRemote = ctx.lookupStateful(CLIENT_ARCHIVE_NAME, EJBClient.class, EJBClientRemote.class);
            String sourceAddress = ejbClientRemote.connectToRemoteBeanAndGetMyIp();
            System.out.println("---- client address :: " + sourceAddress);
            Assert.assertTrue("client should be bound to 127.0.0.10, but is: " + sourceAddress, sourceAddress.contains("127.0.0.10"));
        }
    }

    @Test
    @OperateOnDeployment(CLIENT_ARCHIVE_NAME)
    @InSequence(25)
    public void unset(@ArquillianResource ManagementClient managementClient) throws IOException, InterruptedException {
        ModelNode op = ModelUtil.createOpNode("subsystem=io/worker=default/outbound-bind-address=default", "remove");
        ModelUtil.execute(managementClient, op);
        ManagementOperations.reloadServer();
    }
}
