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

package org.wildfly.ejbclient.testsuite.integration.multinode.servertocluster.security;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class IntermediaryBean implements IntermediaryBeanRemote {

    @EJB(lookup = "ejb:/secured/SecuredBean!org.wildfly.ejbclient.testsuite.integration.multinode.servertocluster.security.SecuredBeanRemote")
    SecuredBeanRemote securedBean;

    @Override
    public String callRemoteSecuredBean() {
        return securedBean.whoAmI();
    }

}
