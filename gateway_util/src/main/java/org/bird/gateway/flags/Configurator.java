package org.bird.gateway.flags;



import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.utils.ApiUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Data
@Slf4j
public class Configurator {
    private static final ApiUtils gatewayApiUtils = new ApiUtils();
    private static final String PROPERTIES_NAME = new File("/config","application.properties").toString();
    protected static Flags instance;


    public static Flags configurator() {
        if (instance == null) {
            instance = setFlags();
        }
        return instance;
    }


    private static Flags setFlags(){
        FileInputStream in = null;
        try {
            Flags flags = new Flags();
            String confPath = System.getProperty("user.dir");
            log.info("ConfigPath:" + confPath);

            Properties properties = new Properties();
            in = new FileInputStream(confPath+PROPERTIES_NAME);
            properties.load(in);

            flags.setClientUID(properties.getProperty("client.uid"));

            if(Boolean.parseBoolean(properties.getProperty("client.online"))){
                flags.setArchiveAddress(properties.getProperty("archive.address"));
                flags.setArchiveUrl(properties.getProperty("archive.url"));
            }

            flags.setMppsUrl(properties.getProperty("mpps.url"));
            flags.setDimseAET(properties.getProperty("dimse.aet"));
            flags.setDimsePort(Integer.valueOf(properties.getProperty("dimse.port")));
            flags.setFileRetry(properties.getProperty("file.retry"));
            flags.setFileUploadCache(properties.getProperty("file.upload.cache"));
            flags.setFileUploadRetry(Integer.valueOf(properties.getProperty("file.upload.retry")));
            flags.setFileMpps(properties.getProperty("file.mpps"));
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
