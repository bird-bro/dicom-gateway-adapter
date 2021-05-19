package org.bird.store;

import lombok.extern.slf4j.Slf4j;
import org.bird.adapter.DicomRedactor;
import org.bird.gateway.IGatewayClient;
import org.bird.store.config.Flags;
import org.bird.store.config.SetConfig;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
public class Adapter {

    private static final String STUDIES = "studies";
    private static final String FILTER = "filter";

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        SetConfig config = new SetConfig();
        config.setConfig();
        log.info("配置文件加载完成！");
        String gatewayAddress = config.getFlags().getGatewayAddress();

        // Dicom service handlers
        DicomServiceRegistry dicomServiceRegistry = new DicomServiceRegistry();
        // Handle C-ECHO (all nodes which accept associations must support this)
        dicomServiceRegistry.addDicomService(new BasicCEchoSCP());
        // Handle C-STORE
        String cstoreAddr = gatewayAddress;
        String cstorePath = STUDIES;
        String cstoreAet = config.getFlags().getDimseAET();

        if (cstoreAet == null || cstoreAet.isBlank()) {
            throw new IllegalArgumentException("--未设置dimse_aet.");
        }

        IGatewayClient defaultCstoreGatewayClient = configureDefaultGatewayClient(cstoreAddr,cstorePath,config.getFlags());

        DicomRedactor redactor = configureRedactor(config.getFlags());






    }


    private static IGatewayClient configureDefaultGatewayClient(String cstoreDicomwebAddr, String cstoreDicomwebStowPath, Flags flags){

    }


    private static DicomRedactor configureRedactor(Flags flags) throws IOException{

    }














}
