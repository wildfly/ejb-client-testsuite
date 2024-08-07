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

package org.wildfly.ejbclient.testsuite.integration.basic.ejbclient292;

import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.BeanType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectory;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanRemote;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateful;
import org.wildfly.ejbclient.testsuite.integration.basic.echo.echobean.EchoBeanStateless;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.management.ManagementOperations;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Deploy an >64 EJB deployments.
 * Check that EJB client is able to perform invocations on EJBs in all the deployments.
 */
@SuppressWarnings({"ArquillianTooManyDeployment"})
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("FIXME: this test is unstable, causes failures in other tests; verify manually for now; should be fixed somehow in the future")
public class MoreThan64EJBDeploymentsTestCase {

    public static final Integer DEPLOYMENTS_COUNT = 65;

    protected InitialContextDirectory ctx;

    public static WebArchive deployment(int i) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "deployment-" + i + ".war");
        war.addPackage(EchoBeanRemote.class.getPackage());
        return war;
    }

    /**
     * Hint how to generate this using bash:
     * for i in `seq 0 80`; do echo "@Deployment(name = \"deployment-$i\") public static WebArchive deployment$i() {
        return deployment($i);
    }"; done
     */
    @Deployment(name = "deployment-0") public static WebArchive deployment0() {
        return deployment(0);
    }
    @Deployment(name = "deployment-1") public static WebArchive deployment1() {
        return deployment(1);
    }
    @Deployment(name = "deployment-2") public static WebArchive deployment2() {
        return deployment(2);
    }
    @Deployment(name = "deployment-3") public static WebArchive deployment3() {
        return deployment(3);
    }
    @Deployment(name = "deployment-4") public static WebArchive deployment4() {
        return deployment(4);
    }
    @Deployment(name = "deployment-5") public static WebArchive deployment5() {
        return deployment(5);
    }
    @Deployment(name = "deployment-6") public static WebArchive deployment6() {
        return deployment(6);
    }
    @Deployment(name = "deployment-7") public static WebArchive deployment7() {
        return deployment(7);
    }
    @Deployment(name = "deployment-8") public static WebArchive deployment8() {
        return deployment(8);
    }
    @Deployment(name = "deployment-9") public static WebArchive deployment9() {
        return deployment(9);
    }
    @Deployment(name = "deployment-10") public static WebArchive deployment10() {
        return deployment(10);
    }
    @Deployment(name = "deployment-11") public static WebArchive deployment11() {
        return deployment(11);
    }
    @Deployment(name = "deployment-12") public static WebArchive deployment12() {
        return deployment(12);
    }
    @Deployment(name = "deployment-13") public static WebArchive deployment13() {
        return deployment(13);
    }
    @Deployment(name = "deployment-14") public static WebArchive deployment14() {
        return deployment(14);
    }
    @Deployment(name = "deployment-15") public static WebArchive deployment15() {
        return deployment(15);
    }
    @Deployment(name = "deployment-16") public static WebArchive deployment16() {
        return deployment(16);
    }
    @Deployment(name = "deployment-17") public static WebArchive deployment17() {
        return deployment(17);
    }
    @Deployment(name = "deployment-18") public static WebArchive deployment18() {
        return deployment(18);
    }
    @Deployment(name = "deployment-19") public static WebArchive deployment19() {
        return deployment(19);
    }
    @Deployment(name = "deployment-20") public static WebArchive deployment20() {
        return deployment(20);
    }
    @Deployment(name = "deployment-21") public static WebArchive deployment21() {
        return deployment(21);
    }
    @Deployment(name = "deployment-22") public static WebArchive deployment22() {
        return deployment(22);
    }
    @Deployment(name = "deployment-23") public static WebArchive deployment23() {
        return deployment(23);
    }
    @Deployment(name = "deployment-24") public static WebArchive deployment24() {
        return deployment(24);
    }
    @Deployment(name = "deployment-25") public static WebArchive deployment25() {
        return deployment(25);
    }
    @Deployment(name = "deployment-26") public static WebArchive deployment26() {
        return deployment(26);
    }
    @Deployment(name = "deployment-27") public static WebArchive deployment27() {
        return deployment(27);
    }
    @Deployment(name = "deployment-28") public static WebArchive deployment28() {
        return deployment(28);
    }
    @Deployment(name = "deployment-29") public static WebArchive deployment29() {
        return deployment(29);
    }
    @Deployment(name = "deployment-30") public static WebArchive deployment30() {
        return deployment(30);
    }
    @Deployment(name = "deployment-31") public static WebArchive deployment31() {
        return deployment(31);
    }
    @Deployment(name = "deployment-32") public static WebArchive deployment32() {
        return deployment(32);
    }
    @Deployment(name = "deployment-33") public static WebArchive deployment33() {
        return deployment(33);
    }
    @Deployment(name = "deployment-34") public static WebArchive deployment34() {
        return deployment(34);
    }
    @Deployment(name = "deployment-35") public static WebArchive deployment35() {
        return deployment(35);
    }
    @Deployment(name = "deployment-36") public static WebArchive deployment36() {
        return deployment(36);
    }
    @Deployment(name = "deployment-37") public static WebArchive deployment37() {
        return deployment(37);
    }
    @Deployment(name = "deployment-38") public static WebArchive deployment38() {
        return deployment(38);
    }
    @Deployment(name = "deployment-39") public static WebArchive deployment39() {
        return deployment(39);
    }
    @Deployment(name = "deployment-40") public static WebArchive deployment40() {
        return deployment(40);
    }
    @Deployment(name = "deployment-41") public static WebArchive deployment41() {
        return deployment(41);
    }
    @Deployment(name = "deployment-42") public static WebArchive deployment42() {
        return deployment(42);
    }
    @Deployment(name = "deployment-43") public static WebArchive deployment43() {
        return deployment(43);
    }
    @Deployment(name = "deployment-44") public static WebArchive deployment44() {
        return deployment(44);
    }
    @Deployment(name = "deployment-45") public static WebArchive deployment45() {
        return deployment(45);
    }
    @Deployment(name = "deployment-46") public static WebArchive deployment46() {
        return deployment(46);
    }
    @Deployment(name = "deployment-47") public static WebArchive deployment47() {
        return deployment(47);
    }
    @Deployment(name = "deployment-48") public static WebArchive deployment48() {
        return deployment(48);
    }
    @Deployment(name = "deployment-49") public static WebArchive deployment49() {
        return deployment(49);
    }
    @Deployment(name = "deployment-50") public static WebArchive deployment50() {
        return deployment(50);
    }
    @Deployment(name = "deployment-51") public static WebArchive deployment51() {
        return deployment(51);
    }
    @Deployment(name = "deployment-52") public static WebArchive deployment52() {
        return deployment(52);
    }
    @Deployment(name = "deployment-53") public static WebArchive deployment53() {
        return deployment(53);
    }
    @Deployment(name = "deployment-54") public static WebArchive deployment54() {
        return deployment(54);
    }
    @Deployment(name = "deployment-55") public static WebArchive deployment55() {
        return deployment(55);
    }
    @Deployment(name = "deployment-56") public static WebArchive deployment56() {
        return deployment(56);
    }
    @Deployment(name = "deployment-57") public static WebArchive deployment57() {
        return deployment(57);
    }
    @Deployment(name = "deployment-58") public static WebArchive deployment58() {
        return deployment(58);
    }
    @Deployment(name = "deployment-59") public static WebArchive deployment59() {
        return deployment(59);
    }
    @Deployment(name = "deployment-60") public static WebArchive deployment60() {
        return deployment(60);
    }
    @Deployment(name = "deployment-61") public static WebArchive deployment61() {
        return deployment(61);
    }
    @Deployment(name = "deployment-62") public static WebArchive deployment62() {
        return deployment(62);
    }
    @Deployment(name = "deployment-63") public static WebArchive deployment63() {
        return deployment(63);
    }
    @Deployment(name = "deployment-64") public static WebArchive deployment64() {
        return deployment(64);
    }
    @Deployment(name = "deployment-65") public static WebArchive deployment65() {
        return deployment(65);
    }
    @Deployment(name = "deployment-66") public static WebArchive deployment66() {
        return deployment(66);
    }
    @Deployment(name = "deployment-67") public static WebArchive deployment67() {
        return deployment(67);
    }
    @Deployment(name = "deployment-68") public static WebArchive deployment68() {
        return deployment(68);
    }
    @Deployment(name = "deployment-69") public static WebArchive deployment69() {
        return deployment(69);
    }
    @Deployment(name = "deployment-70") public static WebArchive deployment70() {
        return deployment(70);
    }
    @Deployment(name = "deployment-71") public static WebArchive deployment71() {
        return deployment(71);
    }
    @Deployment(name = "deployment-72") public static WebArchive deployment72() {
        return deployment(72);
    }
    @Deployment(name = "deployment-73") public static WebArchive deployment73() {
        return deployment(73);
    }
    @Deployment(name = "deployment-74") public static WebArchive deployment74() {
        return deployment(74);
    }
    @Deployment(name = "deployment-75") public static WebArchive deployment75() {
        return deployment(75);
    }
    @Deployment(name = "deployment-76") public static WebArchive deployment76() {
        return deployment(76);
    }
    @Deployment(name = "deployment-77") public static WebArchive deployment77() {
        return deployment(77);
    }
    @Deployment(name = "deployment-78") public static WebArchive deployment78() {
        return deployment(78);
    }
    @Deployment(name = "deployment-79") public static WebArchive deployment79() {
        return deployment(79);
    }
    @Deployment(name = "deployment-80") public static WebArchive deployment80() {
        return deployment(80);
    }

    @Before
    public void before() throws NamingException {
        ctx = new InitialContextDirectory.Supplier().get();
    }

    @After
    public void after() {
        ctx.close();
    }

    private static Logger logger = Logger.getLogger(MoreThan64EJBDeploymentsTestCase.class.getName());

    @Test(timeout = 600000)
    public void test() throws Exception {
        /* reload is necessary to trigger EJBCLIENT-292 because it will force the server
           to send a complete deployment list to the client. Otherwise, if any other tests were
           run before this one, client would only get incremental updates over time from the server,
           and the client would never get a message about 64+ deployments being present, so EJBCLIENT-292
           would not be triggered unless this is the first (or only) test case being run from the test suite.
         */
        ManagementOperations.reloadServer(60000L); // don't remove!
        // FIXME we should find a way to determine when the server is completely started - with this number of deployments
        // it sometimes happens that we detect that the server is ready (we can already execute
        // management operations), but the deployments are not deployed yet, for now just give the server some additional time
        logger.info("Giving the server some time to finish all deployments before starting invocations...");
        TimeUnit.SECONDS.sleep(30);
        for (int i = 0; i < DEPLOYMENTS_COUNT; i++) {
            logger.info("Looking up stateful EJB in deployment number " + i);
            final EchoBeanRemote beanStateful = ctx
                    .lookup(null, "deployment-" + i, EchoBeanStateful.class, EchoBeanRemote.class, BeanType.STATEFUL, null);
            logger.info("Calling stateful EJB in deployment number " + i);
            Assert.assertEquals(8, beanStateful.echo(8));

            logger.info("Looking up stateless EJB in deployment number " + i);
            final EchoBeanRemote beanStateless = ctx
                    .lookup(null, "deployment-" + i, EchoBeanStateless.class, EchoBeanRemote.class, BeanType.STATELESS, null);
            logger.info("Calling stateless EJB in deployment number " + i);
            Assert.assertEquals(8, beanStateless.echo(8));
        }
    }
}
