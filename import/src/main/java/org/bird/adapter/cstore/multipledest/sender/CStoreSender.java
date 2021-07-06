package org.bird.adapter.cstore.multipledest.sender;

import org.bird.adapter.AetDictionary.Aet;
import org.bird.adapter.DicomClient;
import org.dcm4che3.net.ApplicationEntity;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author bird
 * @date 2021-7-2 17:42
 **/
public class CStoreSender implements Closeable  {

    private final ApplicationEntity applicationEntity;


    public CStoreSender(ApplicationEntity applicationEntity) {
        this.applicationEntity = applicationEntity;
    }


    public void cstore(Aet target, String sopInstanceUid, String sopClassUid,
                       InputStream inputStream) throws IOException, InterruptedException {
        DicomClient.connectAndCstore(
                sopClassUid,
                sopInstanceUid,
                inputStream,
                applicationEntity,
                target.getName(),
                target.getHost(),
                target.getPort());
    }

    @Override
    public void close() {
        applicationEntity.getDevice().unbindConnections();
    }


}
