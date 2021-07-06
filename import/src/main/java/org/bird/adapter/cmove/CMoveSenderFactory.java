package org.bird.adapter.cmove;

import lombok.extern.slf4j.Slf4j;
import org.bird.adapter.utils.DeviceUtils;
import org.bird.gateway.IGatewayClient;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;

/**
 * @author bird
 * @date 2021-7-5 13:40
 **/
@Slf4j
public class CMoveSenderFactory implements ISenderFactory {

    private final String cstoreSubAet;
    private final IGatewayClient gatewayClient;


    public CMoveSenderFactory(String cstoreSubAet, IGatewayClient gatewayClient) {
        this.cstoreSubAet = cstoreSubAet;
        this.gatewayClient = gatewayClient;
    }


    @Override
    public ISender create() {
        ApplicationEntity subApplicationEntity = new ApplicationEntity(cstoreSubAet);
        Connection conn = new Connection();
        DeviceUtils.createClientDevice(subApplicationEntity, conn);
        subApplicationEntity.addConnection(conn);

        return new CMoveSender(subApplicationEntity, gatewayClient);
    }

}
