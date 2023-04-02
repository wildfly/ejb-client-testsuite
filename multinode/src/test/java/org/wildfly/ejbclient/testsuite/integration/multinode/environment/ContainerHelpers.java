package org.wildfly.ejbclient.testsuite.integration.multinode.environment;

import org.jboss.arquillian.container.test.api.ContainerController;

public class ContainerHelpers {

    public static void stopContainers(ContainerController controller, String... containers) {
        for (String container : containers) {
            if (controller.isStarted(container)) {
                controller.stop(container);
            }
        }
    }

}
