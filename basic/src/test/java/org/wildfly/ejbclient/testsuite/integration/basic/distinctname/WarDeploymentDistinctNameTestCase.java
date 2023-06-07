/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.ejbclient.testsuite.integration.basic.distinctname;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Tests that the distinct-name configured in the jboss-web.xml of a WAR deployment is taken into
 * consideration during remote EJB invocations.
 *
 * @author Jaikiran Pai // deliberately stolen verbatim from AS7 testsuite by jmartisk
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WarDeploymentDistinctNameTestCase extends DistinctNameTestCase {


    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "distinct-name-in-jboss-web-xml";
    private static final String MODULE_NAME = "remote-ejb-distinct-name-war-test-case";


    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3.jar");
        jar.addPackage(JarDeploymentDistinctNameTestCase.class.getPackage());
        jar.addAsManifestResource("distinctname/jboss-ejb3.xml", "jboss-ejb3.xml");
        
        final WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addAsLibrary(jar);
        war.addAsWebResource("distinctname/jboss-web.xml", "WEB-INF/jboss-web.xml");
        return war;
    }


    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    protected String getDistinctName() {
        return DISTINCT_NAME;
    }

}
