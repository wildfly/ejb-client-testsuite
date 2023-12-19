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

[Basic client tests](basic) and [multinode tests](multinode) can be started with one those options:

* Use bom files. With this configuration, the documentation above describes `-Dserver.home` or `server.zip` parameters. As an alternative for using one of those properties, you can also build server with the latest upstream bits by `mvn -B -ntp package -DspecificModule=prepare` command and then not use `-Dserver.home` or `server.zip` parameters.
* Use dependencies from SERVER_HOME/bin/client/jboss-client.jar.
