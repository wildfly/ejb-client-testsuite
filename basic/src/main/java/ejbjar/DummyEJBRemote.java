package ejbjar;

import jakarta.ejb.Remote;

/**
 * @author Jan Martiska
 */
@Remote
public interface DummyEJBRemote {

    String sayAJoke();

}
