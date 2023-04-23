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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A fully serializable wrapper (Timer itself is NOT serializable) which can be sent via RMI and inspected. Should container all
 * necessary information to determine if timer timeouted on time in each invocation.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
public class TimerWrapper implements Serializable {

    private static final long serialVersionUID = 6954472631859585674L;

    private long timerInterval;
    private List<Long> actualTimeouts;
    private List<Long> expectedTimeouts;
    private String info;

    public TimerWrapper(Date firstTimeout, long interval, String info) {
        this.timerInterval = interval;
        this.actualTimeouts = new ArrayList<>();
        this.expectedTimeouts = new ArrayList<>();
        this.expectedTimeouts.add(firstTimeout.getTime());
        this.info = info;
    }

    public TimerWrapper(long interval, String info, List<Long> expectedTimeouts, List<Long> actualTimeouts) {
        this.timerInterval = interval;
        this.actualTimeouts = actualTimeouts;
        this.expectedTimeouts = expectedTimeouts;
        this.info = info;
    }

    public long getTimerInterval() {
        return timerInterval;
    }

    public Long getFirstTimeout() {
        return expectedTimeouts.get(0);
    }

    public List<Long> getActualTimeouts() {
        return actualTimeouts;
    }

    public List<Long> getExpectedTimeouts() {
        return expectedTimeouts;
    }

    public void addTimeout(Long timeout, Long nextExpectedTimeout) {
        actualTimeouts.add(timeout);
        expectedTimeouts.add(nextExpectedTimeout);
    }

    public String getInfo() {
        return info;
    }

    public static TimerWrapper mergeTimerResults(TimerWrapper first, TimerWrapper second) {
        // verify that this was invoked on same timers
        if (!first.getInfo().equals(second.getInfo())) {
            throw new IllegalStateException("You can only merge two TimerWrappers which relate to same timer! E.g. their info has to be equal.");
        }

        // merge and sort the lists of timeouts
        List<Long> mergedExpectedTimeouts = new ArrayList<>();
        mergedExpectedTimeouts.addAll(first.getExpectedTimeouts());
        mergedExpectedTimeouts.addAll(second.getExpectedTimeouts());
        Collections.sort(mergedExpectedTimeouts);

        List<Long> mergedActualTimeouts = new ArrayList<>();
        mergedActualTimeouts.addAll(first.getActualTimeouts());
        mergedActualTimeouts.addAll(second.getActualTimeouts());
        Collections.sort(mergedActualTimeouts);

        return new TimerWrapper(first.getTimerInterval(), first.getInfo(), mergedExpectedTimeouts, mergedActualTimeouts);
    }
}
