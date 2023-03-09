package org.wildfly.ejbclient.testsuite.integration.basic.compression;

import jakarta.ejb.Stateless;

import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author Jan Martiska
 */
@Stateless
@CompressionHint(compressRequest = true, compressResponse = false)
public class BeanCompressingRequest implements BeanCompressingRequestRemote {

    @Override
    public String echo(String value) {
        return value;
    }

}
