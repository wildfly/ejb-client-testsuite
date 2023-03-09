package ejbjar;

import jakarta.ejb.Stateless;

/**
 * @author Jan Martiska
 */
@Stateless
public class DummyEJB implements DummyEJBRemote {

    @Override
    public String sayAJoke() {
        return "Some joke.";
    }

}
