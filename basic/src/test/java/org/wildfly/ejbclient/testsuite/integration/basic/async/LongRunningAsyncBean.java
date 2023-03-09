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
