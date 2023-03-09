package org.wildfly.ejbclient.testsuite.integration.basic.compression;

import jakarta.ejb.Remote;

import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author Jan Martiska
 */
@Remote
public interface BeanCompressingResponseRemote {

    @CompressionHint(compressRequest = false, compressResponse = true)
    String echo(String value);

}
