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

package org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.client;

import org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;

@Stateful
@TransactionAttribute(value=NOT_SUPPORTED)
public class EJBClient implements EJBClientRemote{

//    @EJB(lookup = "ejb:/source-ip-address-server/IpSourceAddressReturningBean!org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote?stateful")
//    private IpSourceAddressReturningBeanRemote beanRemote;

    @Override
    public String connectToRemoteBeanAndGetMyIp() throws Exception {
        return callBeanByLookup();
    }

    public String callBeanByLookup() throws Exception {
        Properties props = new Properties();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        InitialContext ctx = new InitialContext(props);
        IpSourceAddressReturningBeanRemote remoteBeanByLookup = (IpSourceAddressReturningBeanRemote) ctx
                .lookup("ejb:/source-ip-address-server/IpSourceAddressReturningBean!org.wildfly.ejbclient.testsuite.integration.basic.addressbinding.bean.server.IpSourceAddressReturningBeanRemote?stateful");
        return remoteBeanByLookup.getSourceAddress();
    }
}
