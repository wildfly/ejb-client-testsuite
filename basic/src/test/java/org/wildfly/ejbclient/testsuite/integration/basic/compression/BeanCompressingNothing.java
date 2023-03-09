package org.wildfly.ejbclient.testsuite.integration.basic.compression;

import jakarta.ejb.Stateless;

import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author Jan Martiska
 */
@Stateless
@CompressionHint(compressRequest = false, compressResponse = false)
public class BeanCompressingNothing implements BeanCompressingNothingRemote {

    @Override
    public String echo(String value) {
        return value;
    }

}
