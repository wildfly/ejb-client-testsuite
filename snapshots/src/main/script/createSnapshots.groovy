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

EJB_CLIENT_NAME = "jboss-ejb-client"
EJB_CLIENT_DEFAULT_REPOSITORY =  "https://github.com/wildfly/jboss-ejb-client"
EJB_CLIENT_REPOSITORY_PROPERTY = "ejb.client.repository"
EJB_CLIENT_BRANCH_PROPERTY = "ejb.client.branch"
EJB_CLIENT_VERSION_NAME = "version.org.jboss.ejb-client"
HTTP_CLIENT_NAME = "wildfly-http-client"
HTTP_CLIENT_REPOSITORY = "https://github.com/wildfly/wildfly-http-client"
HTTP_CLIENT_REPOSITORY_PROPERTY = "http.client.repository"
HTTP_CLIENT_BRANCH_PROPERTY = "http.client.branch"
HTTP_CLIENT_VERSION_NAME = "version.org.wildfly.http-client"
WILDFLY_NAME = "wildfly"
WILDFLY_REPOSITORY = "https://github.com/wildfly/wildfly"
WILDFLY_REPOSITORY_PROPERTY = "wildfly.repository"
WILDFLY_BRANCH_PROPERTY = "wildfly.branch"
TESTSUITE_VERSION = "EJB-CLIENT-TESTSUITE"
MVN_BUILD_OPTIONS = "-DskipTests=true"


TARGET_DIR = new File((String)properties['target.dir'])
if (!TARGET_DIR.exists()) {
    TARGET_DIR.mkdir();
}

def ejbClientDir = new File(TARGET_DIR, EJB_CLIENT_NAME)
cloneProject(EJB_CLIENT_REPOSITORY_PROPERTY, EJB_CLIENT_BRANCH_PROPERTY, EJB_CLIENT_DEFAULT_REPOSITORY, EJB_CLIENT_NAME)
setProjectVersion(TESTSUITE_VERSION, ejbClientDir)
buildProject(ejbClientDir)

def httpClientDir = new File(TARGET_DIR, HTTP_CLIENT_NAME)
cloneProject(HTTP_CLIENT_REPOSITORY_PROPERTY, HTTP_CLIENT_BRANCH_PROPERTY, HTTP_CLIENT_REPOSITORY, HTTP_CLIENT_NAME, "2.0")
setProjectVersion(TESTSUITE_VERSION, httpClientDir)
buildProject(httpClientDir)

def wildFlyDir = new File(TARGET_DIR, WILDFLY_NAME)
cloneProject(WILDFLY_REPOSITORY_PROPERTY, WILDFLY_BRANCH_PROPERTY, WILDFLY_REPOSITORY, WILDFLY_NAME)
changeDependencyVersion(EJB_CLIENT_VERSION_NAME, TESTSUITE_VERSION, wildFlyDir)
changeDependencyVersion(HTTP_CLIENT_VERSION_NAME, TESTSUITE_VERSION, wildFlyDir)
setProjectVersion(TESTSUITE_VERSION, wildFlyDir)
// testsuite/test-product-conf has hard-coded version value, that should not be overridden, but it is in previous command. So we need to skip this TS section in this WF build.
// https://github.com/wildfly/wildfly/blob/main/testsuite/test-product-conf/pom.xml
executeCmd("sed -i s|<module>test-product-conf</module>||g testsuite/pom.xml", null, wildFlyDir, false)
buildProject(wildFlyDir, "-Dts.noSmoke")
renameWildFlyBuildDirectory(getProjectVersion(wildFlyDir), wildFlyDir)


def executeCmd(command, env, dir, verbose) {
    println "Executing command " + command + " in directory " + dir
    def execution = command.execute(env, dir)

    if(verbose) {
        execution.waitForProcessOutput(System.out, System.err)
    } else {
        execution.waitForProcessOutput();
    }
}

def cloneProject(String repositoryProperty, String branchProperty, String defaultRepository, String projectName, String defaultBranch = "main") {
    def repository = System.getProperty(repositoryProperty, defaultRepository)
    def branch = System.getProperty(branchProperty, defaultBranch)
    executeCmd("git clone -b ${branch}  --depth=1 --single-branch ${repository} ${projectName}", null, TARGET_DIR, true)
}

def buildProject(projectDir, extraOptions = "") {
    executeCmd("mvn --batch-mode clean install ${MVN_BUILD_OPTIONS} ${extraOptions}", null, projectDir, true)
}

def setProjectVersion(version, projectDir) {
    executeCmd("mvn --batch-mode versions:set -DgenerateBackupPoms=false -DnewVersion=${version}", null, projectDir, false)
}

def getProjectVersion(projectDir) {
    def execution = "mvn help:evaluate -Dexpression=project.version -q -DforceStdout".execute(null, projectDir)
    def result = new StringBuilder()
    execution.waitForProcessOutput(result, System.err)
    return result.toString().split(System.lineSeparator())[0];
}

def changeDependencyVersion(versionProperty, newVersion, projectDir) {
    executeCmd("mvn --batch-mode versions:set-property -Dproperty=${versionProperty} -DnewVersion=${newVersion}", null, projectDir, false)
}

def renameWildFlyBuildDirectory(wildflyVersion, File wildflyDir) {
    def wildFlyBuildDir = new File(wildflyDir, "build/target")
    executeCmd("mv wildfly-${wildflyVersion} wildfly-${TESTSUITE_VERSION}", null, wildFlyBuildDir, false)
    executeCmd("mv wildfly-${wildflyVersion}.zip wildfly-${TESTSUITE_VERSION}.zip", null, wildFlyBuildDir, false)
}


