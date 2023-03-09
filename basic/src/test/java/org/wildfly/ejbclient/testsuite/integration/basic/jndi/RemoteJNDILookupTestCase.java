package org.wildfly.ejbclient.testsuite.integration.basic.jndi;

import java.util.EnumSet;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType.WILDFLY_NAMING_CLIENT;

/**
 * Test remote JNDI lookup without invoking EJBs.
 * TODO add more tests to this
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@ServerSetup({RemoteJNDILookupTestCase.CreateJNDIBindings.class})
@RunAsClient
public class RemoteJNDILookupTestCase {

    @BeforeClass
    public static void onlyForRemoteNaming() {
        Assume.assumeTrue(EnumSet.of(WILDFLY_NAMING_CLIENT).contains(TestEnvironment.getContextType()));
    }

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        return jar;
    }

    @Test
    public void testSimpleStringLookup() throws NamingException {
        final InitialContext ctx = new InitialContextDirectory.Supplier().get().getInitialContext();
        Assert.assertEquals("blabla", ctx.lookup("simple-string"));
    }


    // /subsystem=naming/binding="java:jboss/exported/simple-string":add(type=java.lang.String, value=blabla, binding-type=simple)
    static class CreateJNDIBindings implements ServerSetupTask {

        public static final String SIMPLE_STRING_BINDING_JNDI_ADDR = "java:jboss/exported/simple-string";
        final ModelNode SIMPLE_STRING_BINDING_ADDR = PathAddress.pathAddress("subsystem", "naming")
                .append("binding", SIMPLE_STRING_BINDING_JNDI_ADDR).toModelNode();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            System.out.println("==== creating binding " + SIMPLE_STRING_BINDING_JNDI_ADDR);
            ModelNode addBindingOp = new ModelNode();
            addBindingOp.get(ClientConstants.OP_ADDR).set(SIMPLE_STRING_BINDING_ADDR);
            addBindingOp.get(ClientConstants.OP).set(ClientConstants.ADD);
            addBindingOp.get("type").set("java.lang.String");
            addBindingOp.get("value").set("blabla");
            addBindingOp.get("binding-type").set("simple");

            final ModelNode result = managementClient.getControllerClient().execute(addBindingOp);
            Assert.assertEquals(result.toJSONString(false), ClientConstants.SUCCESS, result.get(ClientConstants.OUTCOME).asString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            System.out.println("==== removing binding " + SIMPLE_STRING_BINDING_JNDI_ADDR);
            ModelNode addBindingOp = new ModelNode();
            addBindingOp.get(ClientConstants.OP_ADDR).set(SIMPLE_STRING_BINDING_ADDR);
            addBindingOp.get(ClientConstants.OP).set(ClientConstants.REMOVE_OPERATION);
            final ModelNode result = managementClient.getControllerClient().execute(addBindingOp);
            Assert.assertEquals(result.toJSONString(false), ClientConstants.SUCCESS, result.get(ClientConstants.OUTCOME).asString());
        }
    }

}
