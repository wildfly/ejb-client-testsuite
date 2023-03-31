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
HEAD_VERSION = "EJB-CLIENT-TESTSUITE"
MVN_BUILD_OPTIONS = "-DskipTests=true"


TARGET_DIR = new File("target")
if (!TARGET_DIR.exists()) {
    TARGET_DIR.mkdir();
}

def ejbClientDir = new File(TARGET_DIR, EJB_CLIENT_NAME)
cloneProject(EJB_CLIENT_REPOSITORY_PROPERTY, EJB_CLIENT_BRANCH_PROPERTY, EJB_CLIENT_DEFAULT_REPOSITORY)
setProjectVersion(HEAD_VERSION, ejbClientDir)
buildProject(ejbClientDir)

def httpClientDir = new File(TARGET_DIR, HTTP_CLIENT_NAME)
cloneProject(HTTP_CLIENT_REPOSITORY_PROPERTY, HTTP_CLIENT_BRANCH_PROPERTY, HTTP_CLIENT_REPOSITORY)
setProjectVersion(HEAD_VERSION, httpClientDir)
buildProject(httpClientDir)

def wildfyDir = new File(TARGET_DIR, WILDFLY_NAME)
cloneProject(WILDFLY_REPOSITORY_PROPERTY, WILDFLY_BRANCH_PROPERTY, WILDFLY_REPOSITORY)
changeDependencyVersion(EJB_CLIENT_VERSION_NAME, HEAD_VERSION, wildfyDir)
changeDependencyVersion(HTTP_CLIENT_VERSION_NAME, HEAD_VERSION, wildfyDir)
buildProject(wildfyDir)


def executeCmd(command, env, dir, verbose) {
    println "Executing command " + command + " in directory " + dir
    def execution = command.execute(env, dir)

    if(verbose) {
        execution.waitForProcessOutput(System.out, System.err)
    } else {
        execution.waitForProcessOutput();
    }
}

def cloneProject(String repositoryProperty, String branchProperty, String defaultRepository) {
    def repository = System.getProperty(repositoryProperty, defaultRepository)
    def branch = System.getProperty(branchProperty, "main")
    executeCmd("git clone -b ${branch} --single-branch ${repository}", null, TARGET_DIR, true)
}

def buildProject(projectDir) {
    executeCmd("mvn clean install ${MVN_BUILD_OPTIONS}", null, projectDir, true)
}

def setProjectVersion(version, projectDir) {
    executeCmd("mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${version}", null, projectDir, false)
}

def changeDependencyVersion(versionProperty, newVersion, projectDir) {
    executeCmd("mvn versions:set-property -Dproperty=${versionProperty} -DnewVersion=${newVersion}", null, projectDir, false)
}


