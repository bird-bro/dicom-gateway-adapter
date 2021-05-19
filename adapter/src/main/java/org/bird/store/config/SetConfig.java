package org.bird.store.config;



import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Data
@Slf4j
public class SetConfig {

    private static final String PROPERTIES_NAME = new File("/config","application.properties").toString();
    Flags flags = new Flags();


    public void setConfig(){
        FileInputStream in = null;
        try {
            String confPath = System.getProperty("user.dir");
            log.info("ConfigPath: " + confPath);

            Properties properties = new Properties();
            in = new FileInputStream(confPath+PROPERTIES_NAME);
            properties.load(in);

            flags.setClientUID(properties.getProperty("client.uid"));
            flags.setDimseAET(properties.getProperty("dims.aet"));
            flags.setDimsePort(Integer.valueOf(properties.getProperty("dimse.port")));
            flags.setGatewayAddress(properties.getProperty("gateway.address"));
            flags.setGatewayUrl(properties.getProperty("gateway.url"));
            flags.setFileUploadCache(properties.getProperty("file.upload.cache"));
            flags.setFileUploadRetry(Integer.valueOf(properties.getProperty("file.upload.retry")));
            flags.setTranscodeToSyntax(properties.getProperty("file.transcode"));



        }catch (IOException e){
            log.error(e.getMessage());
        }finally {
            if(in != null){
                try {
                    in.close();
                }catch (IOException e){
                    log.error(e.getMessage());
                }
            }
        }
    }














}
