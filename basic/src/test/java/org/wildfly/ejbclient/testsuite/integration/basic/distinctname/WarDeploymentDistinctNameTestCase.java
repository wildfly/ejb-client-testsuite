/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
