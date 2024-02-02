# EJB test suite

This is upstream EJB integration TS.

# Quick start

The following commands starts the whole TS with latest upstream bits:

```bash
mvn -B -ntp package -DspecificModule=prepare
mvn -B -ntp dependency:tree clean verify --fail-at-end
```

# TS structure

There are three modules in this TS, see the details in following links:

* [basic client tests](basic)
* [multinode tests](multinode)
* [timers tests](timers)

[Basic client tests](basic) and [multinode tests](multinode) can be started with one of those options which determines where to get EJB client dependencies for testing:

* Use BOMs to govern client dependencies. With this configuration, the documentation above describes `-Dserver.home` or `-Dserver.zip.url` parameters. As an alternative for using one of those properties, you can also build server with the latest upstream bits by `mvn -B -ntp package -DspecificModule=prepare` command and then not use `-Dserver.home` or `-Dserver.zip` parameters.
* Use dependencies from SERVER_HOME/bin/client/jboss-client.jar.
