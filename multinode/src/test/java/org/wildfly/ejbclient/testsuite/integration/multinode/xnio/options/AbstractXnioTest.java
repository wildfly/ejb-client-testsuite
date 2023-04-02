/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.ejbclient.testsuite.integration.multinode.xnio.options;

import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.Containers.NODE1;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.createCreaper;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.ManagementHelpers.setLoggerPrefix;
import static org.wildfly.ejbclient.testsuite.integration.multinode.environment.MiscHelpers.isIPv6;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.ejbclient.testsuite.integration.multinode.environment.DeploymentHelpers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * An abstract class handling most boilerplate code around XNIO opts testing
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public abstract class AbstractXnioTest {

    @ArquillianResource
    private ContainerController containerController;

    OnlineManagementClient creaper;

    protected final JavaArchive DEPLOYMENT = createDeployment();

    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "dummy-bean.jar");
        jar.addClasses(DummyBean.class, DummyBeanImpl.class);
        return jar;
    }

    protected void setWildflyConfigXmlProperty(String name) {
        if ( !isIPv6() ) {
			System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
            "org/wildfly/ejbclient/testsuite/integration/multinode/xnio/options/configs/" + name + ".xml").toString());
		} else {
            System.setProperty("wildfly.config.url", ClassLoader.getSystemResource(
            "org/wildfly/ejbclient/testsuite/integration/multinode/xnio/options/configs/" + name + "-ipv6.xml").toString());
		}
    }

    protected void pingEjbBean() throws NamingException {
        final Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        final InitialContext ejbCtx = new InitialContext(properties);
        final DummyBean bean = (DummyBean) ejbCtx
            .lookup("ejb:/dummy-bean/" + DummyBeanImpl.class.getSimpleName() + "!"
                + DummyBean.class.getName());
        bean.ping();
    }

    /**
     * All subclasses implement this to supplement name of XML config they use 
     */
    public abstract String getConfigName();
    
    @Before
    public void before() throws Exception {
        setWildflyConfigXmlProperty(getConfigName());
        setLoggerPrefix("NODE1", NODE1.homeDirectory, NODE1.configurationXmlFile);
        containerController.start(NODE1.nodeName);
        creaper = createCreaper(NODE1.bindAddress, NODE1.managementPort);
        DeploymentHelpers.deploy(DEPLOYMENT, creaper);
    }

    @After
    public void cleanup() throws Exception {
        DeploymentHelpers.undeploy(DEPLOYMENT.getName(), creaper);
        containerController.stop(NODE1.nodeName);
        setLoggerPrefix("", NODE1.homeDirectory, NODE1.configurationXmlFile);
    }
}
