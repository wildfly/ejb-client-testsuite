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

package org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.wildfly.ejbclient.testsuite.integration.basic.utils.jndi.InitialContextDirectoryGlobal;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.EJBClientContextType;
import org.wildfly.ejbclient.testsuite.integration.basic.utils.TestEnvironment;

/**
 * @author ochaloup
 */
public abstract class InitialContextDirectory implements AutoCloseable {

    public static class Supplier implements java.util.function.Supplier<InitialContextDirectory> {

        @Override
        public InitialContextDirectory get() {
            final EJBClientContextType type = TestEnvironment.getContextType();
            try {
                switch (type) {
                case GLOBAL:
                    return new InitialContextDirectoryGlobal();
                case SCOPED:
                    return new InitialContextDirectoryScoped();
                case WILDFLY_NAMING_CLIENT:
                    return new InitialContextDirectoryWildFlyNamingClient();
                default:
                    throw new IllegalStateException(
                            "Unknown InitialContext type (value of context.type property): " + type);
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    protected InitialContext ctx;

    /**
     * Looking for a bean
     *
     * @param applicationName       name of EAR file where the bean is placed to, whether does not exist pass null
     * @param moduleName       name of JAR file where the bean is placed to
     * @param beanClass     class to be looked for (there could be better to say Class<? extends T> but it does not work for EJB2 beans)
     * @param beanInterface interface of the class
     * @param beanType      type of bean that we are searching for
     * @param distinctName
     * @return returning remote stub interface
     */
    public abstract <T> T lookup(String applicationName, String moduleName, Class<? extends T> beanClass, Class<T> beanInterface,
                                 BeanType beanType, String distinctName) throws NamingException;

    /**
     * Looking for stateful bean in ear.
     */
    public <T> T lookupStateful(String applicationName, String moduleName, Class<? extends T> beanClass,
                                Class<T> beanInterface) throws NamingException {
        return lookup(applicationName, moduleName, beanClass, beanInterface, BeanType.STATEFUL, null);
    }

    /**
     * Looking for stateful bean in ear, with distinct-name
     */
    public <T> T lookupStatefulWithDN(String applicationName, String moduleName, Class<? extends T> beanClass,
                                      Class<T> beanInterface, String distinctName) throws NamingException {
        return lookup(applicationName, moduleName, beanClass, beanInterface, BeanType.STATEFUL, distinctName);
    }

    /**
     * Looking for stateful bean in jar.
     */
    public <T> T lookupStateful(String moduleName, Class<? extends T> beanClass, Class<T> beanInterface)
            throws NamingException {
        return lookupStateful(null, moduleName, beanClass, beanInterface);
    }

    /**
     * Looking for stateful bean in jar, with distinct-name
     */
    public <T> T lookupStatefulWithDN(String moduleName, Class<? extends T> beanClass, Class<T> beanInterface,
                                      String distinctName) throws NamingException {
        return lookupStatefulWithDN(null, moduleName, beanClass, beanInterface, distinctName);
    }

    /**
     * Looking for stateless bean in ear.
     */
    public <T> T lookupStateless(String applicationName, String moduleName, Class<? extends T> beanClass,
                                 Class<T> beanInterface) throws NamingException {
        return lookup(applicationName, moduleName, beanClass, beanInterface, BeanType.STATELESS, null);
    }

    /**
     * Looking for stateless bean in ear, with distinct-name
     */
    public <T> T lookupStatelessWithDN(String applicationName, String moduleName, Class<? extends T> beanClass,
                                       Class<T> beanInterface, String distinctName) throws NamingException {
        return lookup(applicationName, moduleName, beanClass, beanInterface, BeanType.STATELESS, distinctName);
    }

    /**
     * Looking for stateless bean in jar.
     */
    public <T> T lookupStateless(String moduleName, Class<? extends T> beanClass, Class<T> beanInterface)
            throws NamingException {
        return lookupStateless(null, moduleName, beanClass, beanInterface);
    }

    /**
     * Looking for stateless bean in jar.
     */
    public <T> T lookupStatelessWithDN(String moduleName, Class<? extends T> beanClass, Class<T> beanInterface,
                                       String distinctName) throws NamingException {
        return lookupStatelessWithDN(null, moduleName, beanClass, beanInterface, distinctName);
    }


    /**
     * Looking for singleton class in ear.
     */
    public <T> T lookupSingleton(String applicationName, String moduleName, Class<? extends T> beanClass,
                                 Class<T> beanInterface) throws NamingException {
        return lookup(applicationName, moduleName, beanClass, beanInterface, BeanType.SINGLETON, null);
    }

    /**
     * Looking for singleton class in ear, with distinct-name
     */
    public <T> T lookupSingletonWithDN(String applicationName, String moduleName, Class<? extends T> beanClass,
                                       Class<T> beanInterface, String distinctName) throws NamingException {
        return lookup(applicationName, moduleName, beanClass, beanInterface, BeanType.SINGLETON, distinctName);
    }

    /**
     * Looking for singleton class in jar.
     */
    public <T> T lookupSingleton(String moduleName, Class<? extends T> beanClass, Class<T> beanInterface)
            throws NamingException {
        return lookupSingleton(null, moduleName, beanClass, beanInterface);
    }

    /**
     * Looking for singleton class in jar.
     */
    public <T> T lookupSingletonWithDN(String modulEName, Class<? extends T> beanClass, Class<T> beanInterface,
                                       String distinctName) throws NamingException {
        return lookupSingletonWithDN(null, modulEName, beanClass, beanInterface, distinctName);
    }


    public <T> T getBean(String name, Class<T> beanInterface) throws NamingException {
        return beanInterface.cast(this.ctx.lookup(name));
    }

    // subclasses should implement this themselves because not all context types should be closed after using
    public abstract  void close();

    public InitialContext getInitialContext() {
        return ctx;
    }

    public void discardReferenceToInitialContext() {
        ctx = null;
    }
}
