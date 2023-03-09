package org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author Jan Martiska
 */
public class GracefulShutdownHelper {

    public static final ModelNode OPERATION_SUSPEND;

    public static final ModelNode OPERATION_RESUME;

    static {
        OPERATION_SUSPEND = new ModelNode();
        OPERATION_SUSPEND.get(ClientConstants.OP).set("suspend");
        OPERATION_SUSPEND.protect();

        OPERATION_RESUME = new ModelNode();
        OPERATION_RESUME.get(ClientConstants.OP).set("resume");
        OPERATION_RESUME.protect();


    }
}
