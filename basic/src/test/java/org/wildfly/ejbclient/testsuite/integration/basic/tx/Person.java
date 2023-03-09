package org.wildfly.ejbclient.testsuite.integration.basic.tx;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * @author Jan Martiska
 */
@Entity
public class Person implements Serializable {

    @Id
    @GeneratedValue(generator = "person_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "person_seq", sequenceName = "person_seq")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
