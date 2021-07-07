package org.bird.gateway.flags;



import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.utils.GatewayApiUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Data
@Slf4j
public class Configurator {
    private static final GatewayApiUtils gatewayApiUtils = new GatewayApiUtils();
    private static final String PROPERTIES_NAME = new File("/config","application.properties").toString();
    protected static Flags instance;


    public static Flags Configurator() {
        if (instance == null) {
            instance = SetFlags();
        }
        return instance;
    }


    private static Flags SetFlags(){
        FileInputStream in = null;
        try {
            Flags flags = new Flags();
            String confPath = System.getProperty("user.dir");
            log.info("ConfigPath:" + confPath);

            Properties properties = new Properties();
            in = new FileInputStream(confPath+PROPERTIES_NAME);
            properties.load(in);

            flags.setClientAk(properties.getProperty("client.ak"));
            flags.setClientSk(properties.getProperty("client.sk"));
            flags.setClientEnv(properties.getProperty("client.env"));
            flags.setGatewayApi(properties.getProperty("gateway.api"));
            flags.setGatewayApiOauth(properties.getProperty("gateway.api.oauth"));
            flags.setClientUID(properties.getProperty("client.uid"));

            if(Boolean.parseBoolean(properties.getProperty("client.online"))){

            }else {
                flags.setArchiveAddress(properties.getProperty("archive.address"));
                flags.setArchiveUrl(properties.getProperty("archive.url"));
            }

            flags.setDimseAET(properties.getProperty("dimse.aet"));
            flags.setDimsePort(Integer.valueOf(properties.getProperty("dimse.port")));
            flags.setFileRetry(properties.getProperty("file.retry"));
            flags.setFileUploadCache(properties.getProperty("file.upload.cache"));
            flags.setFileUploadRetry(Integer.valueOf(properties.getProperty("file.upload.retry")));
            flags.setTranscodeToSyntax(properties.getProperty("file.transcode"));

            log.info("Configuration loaded successfully!");
            return flags;
        }catch (IOException e){
            log.error("Configuration loaded error! -- {}",e.getMessage());
            return null;
        }finally {
            if(in != null){
                try {
                    in.close();
                }catch (IOException e){
                    log.error("Configuration close error! -- {}",e.getMessage());
                }
            }
        }
    }














}
