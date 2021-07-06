package org.bird.adapter.cmove;

import org.bird.adapter.AetDictionary.Aet;
import org.bird.gateway.IGatewayClient;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author bird
 * @date 2021-7-5 13:41
 **/
public interface ISender extends Closeable {

    /**
     * Sends instance via c-store (or test stub) to target AET, returns bytes sent
     */
    long cmove(
            Aet target,
            String studyUid,
            String seriesUid,
            String sopInstanceUid,
            String sopClassUid)
            throws IGatewayClient.DicomGatewayException, IOException, InterruptedException;

}
