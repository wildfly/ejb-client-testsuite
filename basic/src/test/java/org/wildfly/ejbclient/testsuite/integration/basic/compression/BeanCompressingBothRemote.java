package org.wildfly.ejbclient.testsuite.integration.basic.compression;

import jakarta.ejb.Remote;

import org.jboss.ejb.client.annotation.CompressionHint;

/**
 * @author Jan Martiska
 */
@Remote
public interface BeanCompressingBothRemote {

    @CompressionHint(compressRequest = true, compressResponse = true)
    String echo(String value);

}
