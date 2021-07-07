package org.bird.gateway.retry;

import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.flags.Configurator;
import org.bird.gateway.flags.Flags;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.SafeClose;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author bird
 * @date 2021-7-2 14:53
 **/
@Slf4j
public class DelayStore implements Closeable{

    private static final Flags CONFIG =  Configurator.configurator();
    public final InputStream sourceStream;
    public final  InputStream backupStream;


    public static void toTask(String sopUid, InputStream inputStream) {
        try {
            var retrydDir = CONFIG.getFileRetry();
            if(!retrydDir.isBlank()) {
                File file = new File(retrydDir, sopUid);
                if(!file.exists()) {
                    var successful = file.getParentFile().mkdirs();
                    if (!successful) {
                        log.info("Directory creation failed.");
                    }
                    DicomOutputStream out = new DicomOutputStream(file);
                    try {
                        inputStream.transferTo(out);
                    } finally {
                        SafeClose.close(out);
                    }
                    String path = file.getAbsolutePath();
                    RetryQueue.Put(path);
                }
            }
        }catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }


    public DelayStore(InputStream inputStream) throws IOException {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            sourceStream = new ByteArrayInputStream(baos.toByteArray());
            backupStream = new ByteArrayInputStream(baos.toByteArray());
    }


    public final void toDelayRetry() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        DelayStore.toTask(format.format(new Date()), backupStream);
    }

    @Override
    public void close() throws IOException {
        if (sourceStream != null) {sourceStream.close();}
        if (backupStream != null) {backupStream.close();}
    }



}
