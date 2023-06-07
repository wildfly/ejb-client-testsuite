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

package org.wildfly.ejbclient.testsuite.integration.basic.async;

import java.util.concurrent.Future;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.logging.Logger;

/**
 * @author Jan Martiska
 */
@Stateful
public class LongRunningAsyncBean implements LongRunningAsyncRemote {

    @Resource
    private SessionContext ctx;

    private static Logger logger = Logger.getLogger(LongRunningAsyncBean.class.getName());

    private volatile boolean cancelCalled = false;

    @Override
    @Asynchronous
    public Future<Integer> computeAnswerToEverything() throws InterruptedException {
        for(int i =0; i<30; i++) {
            if(ctx.wasCancelCalled()) {
                logger.info("The cancel method was called!");
                cancelCalled = true;
                return null;
            }
            logger.info("The cancel call not received yet. Sleeping for one second.");
            Thread.sleep(1000);
        }
        return new AsyncResult<>(42);
    }

    public boolean isCancelCalled() {
        return cancelCalled;
    }



}
