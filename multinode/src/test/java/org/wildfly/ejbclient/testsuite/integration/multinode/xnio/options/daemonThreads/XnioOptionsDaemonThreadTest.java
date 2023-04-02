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
package org.wildfly.ejbclient.testsuite.integration.multinode.xnio.options.daemonThreads;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.multinode.xnio.options.AbstractXnioTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.XnioWorker;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XnioOptionsDaemonThreadTest extends AbstractXnioTest {

    @Override
    public String getConfigName() {
        return "daemon-thread";
    }

    @Test
    public void testDaemonThread() throws NamingException {
        // perform some actual EJB call
        pingEjbBean();

        // by default there is just one thread
        Assert.assertEquals(1, XnioWorker.getContextManager().getGlobalDefault().getIoThreadCount());
        // note that the option is by default "true", hence we test for the opposite case
        Assert.assertFalse(XnioWorker.getContextManager().getGlobalDefault().getIoThread().isDaemon());
    }
}
