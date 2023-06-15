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

package org.wildfly.ejbclient.testsuite.integration.basic.compression;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.ConnectorType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CompressionTestCase {

    public static final String ARCHIVE_NAME = "compression-test";

    public static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    /**
     * This is incremented by byteman when a compressed response is received.
     * Reception of a compressed response is recognized by the fact that
     * the method org.jboss.ejb.client.remoting.CompressedMessageHandler.processMessage
     * is called. Each call of that method increments this counter.
     *
     * See the byteman script at ${project.basedir}/byteman/bytemanscript.btm
     */
    public static final AtomicInteger COMPRESSED_INCOMING_MESSAGES_COUNT = new AtomicInteger(0);

    /**
     * Ditto for outgoing messages (requests)
     */
    public static final AtomicInteger COMPRESSED_OUTGOING_MESSAGES_COUNT = new AtomicInteger(0);

    public static final String SOME_STRING = "lalala";

    @BeforeClass
    public static void before() {
        // FIXME
        Assume.assumeFalse("CompressionTestCase currently skipped because it causes hangs",
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTP) ||
                TestEnvironment.getConnectorType().equals(ConnectorType.HTTPS));
    }


    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        archive.addPackage(BeanCompressingBoth.class.getPackage());
        return archive;
    }

    @Before
    public void activateCollection() {
        ENABLED.set(true);
    }

    @After
    public void deactivateCollection() {
        ENABLED.set(false);
    }

    @Test
    public void checkResponseCompression() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingBothRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingBoth.class, BeanCompressingBothRemote.class);
            int compressedIncomingMessagesBefore = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedIncomingMessagesAfter = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed responses detected", compressedIncomingMessagesAfter > compressedIncomingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkRequestCompression() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingBothRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingBoth.class, BeanCompressingBothRemote.class);
            int compressedOutgoingMessagesBefore = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedOutgoingMessagesAfter = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed requests detected", compressedOutgoingMessagesAfter > compressedOutgoingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkResponseCompression_disabled() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingNothingRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingNothing.class, BeanCompressingNothingRemote.class);
            int compressedIncomingMessagesBefore = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedIncomingMessagesAfter = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed responses should be detected", compressedIncomingMessagesAfter == compressedIncomingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkRequestCompression_disabled() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingNothingRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingNothing.class, BeanCompressingNothingRemote.class);
            int compressedOutgoingMessagesBefore = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedOutgoingMessagesAfter = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed requests should be detected", compressedOutgoingMessagesAfter == compressedOutgoingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkResponseCompression_onlyResponse() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingResponseRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingResponse.class, BeanCompressingResponseRemote.class);
            int compressedIncomingMessagesBefore = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedIncomingMessagesAfter = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed responses detected", compressedIncomingMessagesAfter > compressedIncomingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkRequestCompression_onlyResponse() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingResponseRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingResponse.class, BeanCompressingResponseRemote.class);
            int compressedOutgoingMessagesBefore = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedOutgoingMessagesAfter = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed requests should be detected", compressedOutgoingMessagesAfter == compressedOutgoingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkResponseCompression_onlyRequest() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingRequestRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingRequest.class, BeanCompressingRequestRemote.class);
            int compressedIncomingMessagesBefore = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedIncomingMessagesAfter = COMPRESSED_INCOMING_MESSAGES_COUNT.get();
            Assert.assertTrue("No compressed responses should be detected", compressedIncomingMessagesAfter == compressedIncomingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }

    @Test
    public void checkRequestCompression_onlyRequest() throws Exception {
        try (InitialContextDirectory directory = new InitialContextDirectory.Supplier().get()) {
            final BeanCompressingRequestRemote bean = directory
                    .lookupStateless(ARCHIVE_NAME, BeanCompressingRequest.class, BeanCompressingRequestRemote.class);
            int compressedOutgoingMessagesBefore = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            final String response = bean.echo(SOME_STRING);
            int compressedOutgoingMessagesAfter = COMPRESSED_OUTGOING_MESSAGES_COUNT.get();
            Assert.assertTrue("Compressed requests should be detected", compressedOutgoingMessagesAfter > compressedOutgoingMessagesBefore);
            Assert.assertEquals("Unexpected response", "lalala", response);
        }
    }
}
