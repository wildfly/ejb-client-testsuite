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

package org.wildfly.ejbclient.testsuite.integration.basic.tx;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.jboss.logging.Logger;

/**
 * @author Jan Martiska
 */
@Stateless
public class TransactionalBean implements TransactionalBeanRemote {

    private static Logger logger = Logger.getLogger(TransactionalBean.class.getName());

    @PersistenceContext(unitName = "TestingPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void createPerson() {
        em.persist(new Person());
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<Person> getPersonList() {
        final TypedQuery<Person> allPersonsQuery = em.createQuery("select p from Person p", Person.class);
        return allPersonsQuery.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void clean() {
        final Query deleteQuery = em.createQuery("delete from Person p");
        final int deleted = deleteQuery.executeUpdate();
        logger.info("Deleted " + deleted + " persons from db");
    }
}
