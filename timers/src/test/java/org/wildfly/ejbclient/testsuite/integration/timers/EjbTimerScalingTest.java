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
package org.wildfly.ejbclient.testsuite.integration.timers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.URIAffinity;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Note that the timers are ticking as we measure them. That means some might go off even after we are done with last step and
 * other may overlap in those steps due to delays caused by input parameters in combination with code execution time.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbTimerScalingTest {

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    // container-related variables
    private List<String> allContainers;
    protected static final String CONTAINER1 = "container1";
    protected static final String CONTAINER2 = "container2";
    protected static final String DEPLOYMENT1 = "dep.container1";
    protected static final String DEPLOYMENT2 = "dep.container2";

    // test start/end times, steps taken
    private long testEnded;
    private long testStarted;
    private int currentStep = 0;
    Map<Integer, Long> stepStartedMap = new HashMap<>();
    Map<Integer, Long> stepEndedMap = new HashMap<>();

    // Test run settings extracted from system properties
    private Long interval; // interval between timeouts, this is in MILLISECONDS
    private Integer stepSize; // how many timers are created in each step
    private Integer stepDelay; // time between steps, this is in SECONDS
    private Integer testLength; // how long the test should run in total, this is in SECONDS
    private Long maxExecutionDelay; // maximum delay the timer can have between two executions, basically $timerSchedule + some deviation; this is in MILLISECONDS

    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "deployment-test.war");
        war.addPackage(EjbTimerScalingTest.class.getPackage());
        return war;
    }

    @Before
    public void before() throws MalformedURLException {
        allContainers = new ArrayList<>();
        allContainers.add(System.getProperty("node1.contextPath"));
        allContainers.add(System.getProperty("node2.contextPath"));

        // obtain test env settings
        interval = Long.valueOf(System.getProperty("timerSchedule", "5000"));
        stepSize = Integer.valueOf(System.getProperty("stepSize", "50"));
        stepDelay = Integer.valueOf(System.getProperty("stepDelay", "30"));
        testLength = Integer.valueOf(System.getProperty("testLength", "180"));
        maxExecutionDelay = Long.valueOf(System.getProperty("maxExecutionDelay", "500"));
    }

    @After
    public void after() {
        deployer.undeploy(DEPLOYMENT1);
        controller.stop(CONTAINER1);
        deployer.undeploy(DEPLOYMENT2);
        controller.stop(CONTAINER2);
    }

    @Deployment(name = DEPLOYMENT1, managed = false, testable = false)
    @TargetsContainer(CONTAINER1)
    public static WebArchive createTestDeployment1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT2, managed = false, testable = false)
    @TargetsContainer(CONTAINER2)
    public static WebArchive createTestDeployment2() {
        return createDeployment().addAsWebInfResource(EmptyAsset.INSTANCE, "force-hashcode-change.txt");
    }

    @Test
    public void test() throws NamingException, InterruptedException, URISyntaxException, IOException {
        // start containers and deploy app
        controller.start(CONTAINER1);
        Thread.sleep(1000);
        deployer.deploy(DEPLOYMENT1);

        controller.start(CONTAINER2);
        Thread.sleep(1000);
        deployer.deploy(DEPLOYMENT2);

        // we use try block just for the sake of finally block, so that we can clean up stored timers even after error
        try {
            // in order to measure test length, we simply look at the timer when we start and compare in each iteration
            testStarted = System.currentTimeMillis();
            while (System.currentTimeMillis() - testStarted < testLength * 1000) {
                currentStep++;
                stepStartedMap.put(currentStep, System.currentTimeMillis());

                // create timers
                startTimersInOneStep("S" + currentStep);

                // wait given amount of time
                Thread.sleep(stepDelay * 1000);
                stepEndedMap.put(currentStep, System.currentTimeMillis());

                // verify existing timers, cut the test if there is failure
                checkStepResults(currentStep);
            }

            // test over, check time and generate report
            testEnded = System.currentTimeMillis();
            generateReport(null);
        } finally {
            // timer cleanup, should be done on all nodes I suppose?
            for (String nodeUrl : allContainers) {
                StatisticsHoarder hoarder = getStatisticsBeans(nodeUrl);
                EJBClient.setStrongAffinity(hoarder, URIAffinity.forUri(new URI("remote+" + nodeUrl)));
                hoarder.annihilateTimers();
            }
        }

    }

    private Context getContext(String nodeUrl) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        jndiProperties.put(Context.PROVIDER_URL, "remote+" + nodeUrl);
        return new InitialContext(jndiProperties);
    }

    private void startTimersInOneStep(String stepNumber) throws NamingException, URISyntaxException {
        // we want to create timers on all nodes
        int timersPerNode = stepSize / allContainers.size();
        int createdTimer = 0;
        for (int container = 0; container < allContainers.size(); container++) {
            String chosenContainer = allContainers.get(container);
            Context ejbCtx = getContext(chosenContainer);
            final RemoteSLSBInterface bean = (RemoteSLSBInterface) ejbCtx
                .lookup("ejb:/deployment-test//" + SLSBImpl.class.getSimpleName() + "!"
                    + RemoteSLSBInterface.class.getName());
            for (int i = 1; i <= timersPerNode; i++) {
                createdTimer++;
                // reset affinity before calling - this way we make sure we address given server in cluster
                EJBClient.setStrongAffinity(bean, URIAffinity.forUri(new URI("remote+" + chosenContainer)));
                // create timer, the info will always be as Sx:Ty, where x is number of step and y is number of timer in that iteration
                bean.createTimer(interval, interval, stepNumber + ":T" + createdTimer);

            }
            // if (for instance) stepSize is odd and number of containers are even, we need to make sure the remaining timers are created somewhere
            if (container == allContainers.size() - 1 && createdTimer != stepSize) {
                while (createdTimer != stepSize) {
                    createdTimer++;
                    // reset affinity before calling - this way we make sure we address given server in cluster
                    EJBClient.setStrongAffinity(bean, URIAffinity.forUri(new URI("remote+" + chosenContainer)));
                    // create timer, the info will always be as Sx:Ty, where x is number of step and y is number of timer in that iteration
                    bean.createTimer(interval, interval, stepNumber + ":T" + createdTimer);
                }
            }
        }
    }

    private StatisticsHoarder getStatisticsBeans(String nodeUrl) throws NamingException {
        // they are singleton beans, so there is one bean per node
        Context ejbCtx = getContext(nodeUrl);
        return (StatisticsHoarder) ejbCtx
            .lookup("ejb:/deployment-test//" + StatisticsHoarderImpl.class.getSimpleName() + "!"
                + StatisticsHoarder.class.getName());
    }

    private Map<String, TimerWrapper> getAllTimers() throws NamingException, URISyntaxException {
        Map<String, TimerWrapper> timersFromAllNodes = new HashMap<>();
        for (String container : allContainers) {
            StatisticsHoarder hoarder = getStatisticsBeans(container);
            EJBClient.setStrongAffinity(hoarder, URIAffinity.forUri(new URI("remote+" + container)));
            Map<String, TimerWrapper> timersFromNode = hoarder.getAllTimers();
            if (timersFromNode.values().size() == 0) {
                throw new IllegalStateException("All timers were stored on one node, something is fishy!");
            }
            for (String key : timersFromNode.keySet()) {
                if (timersFromAllNodes.keySet().contains(key)) {
                    // if this happens, it means that timer was notified on several nodes during the course of testing and we need to do some merging
                    TimerWrapper previouslyFound = timersFromAllNodes.get(key);
                    TimerWrapper newlyFound = timersFromNode.get(key);
                    timersFromAllNodes.put(key, TimerWrapper.mergeTimerResults(previouslyFound, newlyFound));
                } else {
                    timersFromAllNodes.put(key, timersFromNode.get(key));
                }
            }
        }
        return timersFromAllNodes;
    }

    private void checkStepResults(int stepNumber) throws NamingException, IOException, URISyntaxException {
        // gather all the created timers
        // this has to be done on all nodes and results have to be combined
        Map<String, TimerWrapper> allTimers = getAllTimers();
        // verify the total number of timers
        Assert.assertEquals((stepNumber) * stepSize, allTimers.values().size());

        // for each timer inspect the timeouts
        for (TimerWrapper wrapper : allTimers.values()) {
            for (int i = 0; i < wrapper.getActualTimeouts().size(); i++) {
                long expectedTimeout = wrapper.getExpectedTimeouts().get(i);
                // timeout is within boundaries of given step, we want to check it
                if (expectedTimeout > stepStartedMap.get(stepNumber) && expectedTimeout < stepEndedMap.get(stepNumber)) {
                    long actualTimeout = wrapper.getActualTimeouts().get(i);
                    if (actualTimeout - expectedTimeout > maxExecutionDelay) {
                        testEnded = System.currentTimeMillis();
                        generateReport(stepNumber);
                        Assert.fail("In step number " + stepNumber + ", Timer " + wrapper.getInfo()
                            + " exceeded maxExecutionDelay(" + maxExecutionDelay + "). The actual delay was "
                            + (actualTimeout - expectedTimeout) + "ms!\n"
                            + "Crashed on actual timeout: " + actualTimeout + " while the expected was: " + expectedTimeout + "\n"
                            + "Expected timeouts in this step: " + wrapper.getExpectedTimeouts() + "\n"
                            + "Actual timeouts in this step: " + wrapper.getActualTimeouts());
                    }
                }
            }
        }
    }

    private void generateReport(Integer abruptlyEndedInStep) throws NamingException, IOException, URISyntaxException {
        Map<String, TimerWrapper> allTimers = getAllTimers();

        StringBuilder testReport = new StringBuilder();
        testReport.append("Test Results for EJB timers");
        testReport.append("\n\n");

        if (abruptlyEndedInStep != null) {
            testReport.append("NOTE: This test ended abruptly because the maxExecutionDelay conditition was breached!\n");
            testReport.append("This happened in step " + abruptlyEndedInStep + ".\n\n");
        }

        // TODO there should be more variables in the end
        testReport.append("Test environment setup\n");
        testReport.append("* timerSchedule = ").append(interval).append(" ms").append("\n");
        testReport.append("* stepSize = ").append(stepSize).append(" timers in one step").append("\n");
        testReport.append("* stepDelay = ").append(stepDelay).append(" seconds").append("\n");
        testReport.append("* steps taken = ").append(currentStep).append("\n");
        testReport.append("* testLength = ").append(testLength).append(" seconds").append("\n");
        testReport.append("* REAL test length = ").append((testEnded - testStarted) / 1000).append(" seconds").append("\n");
        testReport.append("* maxExecutionDelay = ").append(maxExecutionDelay).append(" ms").append("\n");
        testReport.append("\n\n");

        for (int step = 1; step <= currentStep; step++) {
            List<Long> allDelays = new ArrayList<>();
            int timersFound = 0;

            // for each timer inspect the timeouts
            for (TimerWrapper wrapper : allTimers.values()) {
                boolean timerActiveInThisStep = false;
                for (int i = 0; i < wrapper.getActualTimeouts().size(); i++) {
                    long expectedTimeout = wrapper.getExpectedTimeouts().get(i);
                    // timeout is within boundaries of given step
                    if (expectedTimeout > stepStartedMap.get(step) && expectedTimeout < stepEndedMap.get(step)) {
                        timerActiveInThisStep = true;
                        long actualTimeout = wrapper.getActualTimeouts().get(i);
                        allDelays.add(actualTimeout - expectedTimeout);
                    }
                }
                if (timerActiveInThisStep) {
                    timersFound++;
                }
            }
            LongSummaryStatistics stats = allDelays.stream().mapToLong((x) -> x).summaryStatistics();

            // now, let's fill in the report
            testReport.append("Step ").append(step)
                .append(", timers active: ").append(timersFound)
                .append(", total number of timeouts in this step: ").append(stats.getCount()).append("\n");
            testReport.append("* Avg. delay: ").append(stats.getAverage()).append(" ms").append("\n");
            testReport.append("* Min. delay: ").append(stats.getMin()).append(" ms").append("\n");
            testReport.append("* Max. delay: ").append(stats.getMax()).append(" ms").append("\n");
            testReport.append("* Step started: ").append(new Date(stepStartedMap.get(step)).toString()).append("\n");
            testReport.append("* Step ended: ").append(new Date(stepEndedMap.get(step)).toString()).append("\n");
            testReport.append("\n\n");
        }

        // create the report file under target/
        // use ${user.dir}/target
        File file = new File(System.getProperty("user.dir") + "/target/testReport");
        if (!file.exists()) {
            file.createNewFile();
        }
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.print(testReport.toString());
        writer.close();
    }
}
