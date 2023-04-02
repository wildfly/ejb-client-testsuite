package org.wildfly.ejbclient.testsuite.integration.multinode.transactions.bean;

import java.util.List;
import jakarta.ejb.Remote;

import org.wildfly.ejbclient.testsuite.integration.multinode.transactions.Person;

/**
 * @author Jan Martiska
 */
@Remote
public interface TransactionalBeanRemote {

    void createPerson();

    List<Person> getPersonList();

    void clean();

    String getNode();

    void dummyMethodWithNotSupportedTransactions();

}
