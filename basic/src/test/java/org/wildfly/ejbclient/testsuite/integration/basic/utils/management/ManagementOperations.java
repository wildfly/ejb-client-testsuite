package org.wildfly.ejbclient.testsuite.integration.basic.utils.management;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.xnio.IoUtils;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.junit.Assert.fail;

/**
 * @author Jan Martiska
 */
public final class ManagementOperations {

    public static final ModelNode REMOTE_SERVICE_ADDRESS =
            PathAddress.pathAddress(ClientConstants.SUBSYSTEM, "ejb3")
                    .append("service", "remote")
                    .toModelNode();

    public static final ModelNode RELOAD_OPERATION;

    static {
        RELOAD_OPERATION = new ModelNode();
        RELOAD_OPERATION.get(OP).set("reload");
        RELOAD_OPERATION.get(OP_ADDR).setEmptyList();
    }



    public static ModelNode createSetExecuteInWorkerOperation(boolean newValue) {
        ModelNode result = new ModelNode();
        result.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        result.get(ADDRESS).set(REMOTE_SERVICE_ADDRESS);
        result.get(NAME).set("execute-in-worker");
        result.get(VALUE).set(newValue);
        return result;
    }

    private static Logger logger = Logger.getLogger(ManagementOperations.class.getName());

    /**
     * Reload the server and wait for this operation to finish (30 second timeout)
     */
    public static void reloadServer() throws IOException, InterruptedException {
        reloadServer(30000L);
    }

    /**
     * Reload the server and wait for this operation to finish within specified timeout
     * @param timeout timeout in milliseconds
     */
    public static void reloadServer(long timeout) throws IOException, InterruptedException {
        try (ModelControllerClient client = ModelControllerClient.Factory
                .create("127.0.0.1", 9990)) {

            try {
                ModelNode result = client.execute(ManagementOperations.RELOAD_OPERATION);
                Assert.assertEquals("success", result.get(ClientConstants.OUTCOME).asString());
            } catch (IOException e) {
                final Throwable cause = e.getCause();
                if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                    throw new RuntimeException(e);
                } // else ignore, this might happen if the channel gets closed before we got the response
            }
        }

        long start = System.currentTimeMillis();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).setEmptyList();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        while (System.currentTimeMillis() - start < timeout) {
            try {
                ModelControllerClient liveClient = ModelControllerClient.Factory.create(
                        "127.0.0.1", 9990);
                try {
                    ModelNode result = liveClient.execute(operation);
                    if ("running" .equals(result.get(RESULT).asString())) {
                        return;
                    }
                } catch (IOException e) {
                } finally {
                    IoUtils.safeClose(liveClient);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        fail("Live Server did not reload in the imparted time.");

    }




}
