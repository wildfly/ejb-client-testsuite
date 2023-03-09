package org.wildfly.ejbclient.testsuite.integration.basic.tx;

import java.util.List;
import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface TransactionalBeanRemote {

    void createPerson();

    List<Person> getPersonList();

    void clean();

}
