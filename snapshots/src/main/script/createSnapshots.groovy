EJB_CLIENT_NAME = "jboss-ejb-client"
EJB_CLIENT_REPOSITORY = "https://github.com/wildfly/jboss-ejb-client"
EJB_CLIENT_VERSION_NAME = "version.org.jboss.ejb-client"
HTTP_CLIENT_NAME = "wildfly-http-client"
HTTP_CLIENT_REPOSITORY = "https://github.com/wildfly/wildfly-http-client"
HTTP_CLIENT_VERSION_NAME = "version.org.wildfly.http-client"
WILDFLY_NAME = "wildfly"
WILDFLY_REPOSITORY = "https://github.com/wildfly/wildfly"
MVN_BUILD_OPTIONS = "-DskipTests=true"

TARGET_DIR = new File("target")
if (!TARGET_DIR.exists()) {
    TARGET_DIR.mkdir();
}

def ejbClientDir = new File(TARGET_DIR, EJB_CLIENT_NAME)
cloneProject(EJB_CLIENT_REPOSITORY)
buildProject(ejbClientDir)
def ejbClientVersion = getProjectVersion(ejbClientDir)

def httpClientDir = new File(TARGET_DIR, HTTP_CLIENT_NAME)
cloneProject(HTTP_CLIENT_REPOSITORY)
buildProject(httpClientDir)
def httpClientVersion = getProjectVersion(httpClientDir)

def wildfyDir = new File(TARGET_DIR, WILDFLY_NAME)
cloneProject(WILDFLY_REPOSITORY)
changeDependencyVersion(EJB_CLIENT_VERSION_NAME, ejbClientVersion, wildfyDir)
changeDependencyVersion(HTTP_CLIENT_VERSION_NAME, httpClientVersion, wildfyDir)
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

def cloneProject(repository) {
    executeCmd("git clone ${repository}", null, TARGET_DIR, true)
}

def buildProject(projectDir) {
    executeCmd("mvn clean install ${MVN_BUILD_OPTIONS}", null, projectDir, true)
}

def getProjectVersion(projectDir) {
    def execution = "mvn help:evaluate -Dexpression=project.version -q -DforceStdout".execute(null, projectDir)
    def result = new StringBuilder()
    execution.waitForProcessOutput(result, System.err)
    return result.toString()
}

def changeDependencyVersion(versionProperty, newVersion, projectDir) {
    executeCmd("mvn versions:set-property -Dproperty=${versionProperty} -DnewVersion=${newVersion}", null, projectDir, false)
}


