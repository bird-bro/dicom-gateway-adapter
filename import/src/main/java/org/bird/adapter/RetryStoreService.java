package org.bird.adapter;

import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.IGatewayClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author bird
 * @date 2021-7-5 15:16
 **/
@Slf4j
public class RetryStoreService {

    private static RetryStoreService retryStoreService = null;
    private final IGatewayClient gatewayClient;


    public static final RetryStoreService getInstance(IGatewayClient gatewayClient) {
        if (retryStoreService == null) {
            retryStoreService = new RetryStoreService(gatewayClient);
        }
        return retryStoreService;
    }

    private RetryStoreService(IGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public void stowRs(String filePath) {
        InputStream inputStream = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) return;
            if(!file.isDirectory()) {
                inputStream = new FileInputStream(file);
                gatewayClient.stowRs(inputStream);
                inputStream.close();
                file.delete();
            }
        } catch (IOException | IGatewayClient.DicomGatewayException e) {
            log.error(e.getMessage());
        }
    }


}
