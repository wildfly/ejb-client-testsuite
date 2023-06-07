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
