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
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps all the data from timeouts and exposes API for test to call
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@Singleton
@Startup
public class StatisticsHoarderImpl implements StatisticsHoarder {

    @Resource
    TimerService service;

    private Map<String, TimerWrapper> allTimers;
    private long timerInterval;
    
    @PostConstruct
    public void init() {
        allTimers = new HashMap<>();
    }

    @Lock(LockType.WRITE)
    @Override
    public void addTimer(Object timerInfo, Date nextTimeout, long interval) {
        if (timerInfo instanceof String) {
            allTimers.put((String) timerInfo, new TimerWrapper(nextTimeout, interval, (String) timerInfo));
        } else {
            throw new IllegalStateException("Timer has to contain an info of type String which will then be used as an ID.");
        }
        if (timerInterval == 0) {
            timerInterval = interval;
        }
    }

    @Lock(LockType.READ)
    @Override
    public TimerWrapper getTimerWrapper(String timerInfo) {
        return allTimers.get(timerInfo);
    }

    @Lock(LockType.WRITE)
    @Override
    public void addTimeout(Object timerInfo, Date nextTimeout, long currentTime) {
        if (!(timerInfo instanceof String)) {
            throw new IllegalStateException("timerInfo has to be of type String!");
        }
        // we might get notified of a timer which ATM has no record on this node, we need to add a new record
        TimerWrapper wrapper = allTimers.get((String) timerInfo);
        if (wrapper == null) {
            wrapper = new TimerWrapper(timerInterval, (String) timerInfo, new ArrayList<Long>(), new ArrayList<Long>());
            allTimers.put((String) timerInfo, wrapper);
        }
        wrapper.addTimeout(currentTime, nextTimeout.getTime());
    }

    @Override
    public void annihilateTimers() {
        for (Timer timer : service.getAllTimers()) {
            try {
                timer.cancel();
            } catch (Exception e) {
                //timers that finished before my cause "no found" exception here - ignored
            }
        }
    }

    @Override
    @Lock(LockType.READ)
    public Map<String, TimerWrapper> getAllTimers() {
        return Collections.unmodifiableMap(allTimers);
    }
}
