package org.wildfly.ejbclient.testsuite.integration.basic.gracefulshutdown.timerservice;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Jan Martiska
 */
@Singleton
@Startup
public class TimedCountingBean implements TimedCountingBeanRemote {

    private int ticks;

    @PostConstruct
    public void init() {
        System.out.println("Initializing TimedCountingBean");
        ticks = 0;
    }

    @Schedule(hour = "*", minute = "*", second = "*", persistent = false)
    public synchronized void tick() {
        System.out.println("Tick! number " + ++ticks);
        try {
            Thread.sleep(700);  // prevent running the timeout bazillion times at once after resuming from a suspend.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getTicks() {
        return ticks;
    }
}
