package org.bird.adapter.cstore.multipledest.sender;

import org.bird.adapter.utils.DeviceUtils;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;

/**
 * @author bird
 * @date 2021-7-2 17:41
 **/
public class CStoreSenderFactory {

    private final String cstoreSubAet;

    public CStoreSenderFactory(String cstoreSubAet) {
        this.cstoreSubAet = cstoreSubAet;
    }

    public CStoreSender create() {
        ApplicationEntity subApplicationEntity = new ApplicationEntity(cstoreSubAet);
        Connection conn = new Connection();
        DeviceUtils.createClientDevice(subApplicationEntity, conn);
        subApplicationEntity.addConnection(conn);

        return new CStoreSender(subApplicationEntity);
    }

}
