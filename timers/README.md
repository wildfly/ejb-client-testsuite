# EJB timer tests

## Description
The goal of this TS is to create a configurable scalability test for EJB timers.
There is also an output generated (currently under `target/` folder of this project).

The test suite will run in steps, each of which will spawn given amount of timers.
Then the tests waits for configurable amount of time and checks the timeouts which occurred.

## How to run

This TS runs on your local machine using two servers.
You will need to provide a path to them:

Other than that, there is a bunch of parameters the test supports.
All of the can be provided as a property, e.g. `-DstepSize=10`.
Here is a list of the parameters:
* timerSchedule
   * How often the timer timeouts
   * Defined in ms
   * Defaults to 5000 ms
* stepSize
   * How many timer instances are created in each step
   * Defaults to 50 timers in each step
* stepDelay
   * How long does each step last, e.g. period where timers are left to repeatedly timeout before state of the test is verified
   * Defined in seconds
   * Defaults to 30 s
* testLength
   * How long will the test run in total
   * Defined in seconds
   * Defaults to 180 s
* maxExecutionDelay
   * What is the allowed delay between when the timer should timeout and when it actually does
   * Crossing this threshold will end the test abruptly as scaling it higher makes no sense
   * Defined in milliseconds
   * Defaults to 500 ms

An example of whole command could be:
`mvn clean verify -Dnode1.jbossHome=/path/to/node1/server -Dnode2.jbossHome=/path/to/node2/server -DstepSize=50 -DtimerSchedule=5000 -DtestLength=180 -DstepDelay=30 -DmaxExecutionDelay=500`

NOTE: Test assumes two server instances, both of which have preconfigured shared DB connection for timers.
