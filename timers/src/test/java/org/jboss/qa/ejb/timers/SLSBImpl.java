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
package org.jboss.qa.ejb.timers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

/**
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@Stateless
public class SLSBImpl implements RemoteSLSBInterface {

    @Resource
    private TimerService service;

    @EJB
    StatisticsHoarder hoarder;

    @Override
    public void createTimer(Long initialDelay, Long interval, String info) {
        // create a timer which goes off after a period of time equal to $interval, then after each $interval
        // it carries an info which serves as an ID later on
        Timer newTimer = service.createIntervalTimer(initialDelay, interval, new TimerConfig(info, true));
        hoarder.addTimer(newTimer.getInfo(), newTimer.getNextTimeout(), interval);
    }

    @Timeout
    public void onTimeout(Timer timer) {
        // the actual time at which this was invoked is simply obtained from System
        hoarder.addTimeout(timer.getInfo(), timer.getNextTimeout(), System.currentTimeMillis());
    }
}
