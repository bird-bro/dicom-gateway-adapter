package org.bird.adapter.cstore.multipledest;

import com.google.common.collect.ImmutableList;
import org.bird.adapter.AetDictionary.Aet;
import org.bird.gateway.IGatewayClient;
import org.bird.adapter.cstore.backup.IBackupUploader.BackupException;
import java.io.InputStream;

/**
 * @author bird
 * @date 2021-7-2 16:44
 **/
public interface IMultipleDestinationUploadService {

    void start(ImmutableList<IGatewayClient> healthcareDestinations,
               ImmutableList<Aet> dicomDestinations,
               InputStream inputStream,
               String sopClassUID,
               String sopInstanceUID) throws MultipleDestinationUploadServiceException;


    class MultipleDestinationUploadServiceException extends Exception {

        private Integer dicomStatus;

        public MultipleDestinationUploadServiceException(Throwable cause) {
            super(cause);
        }

        public MultipleDestinationUploadServiceException(BackupException be) {
            super(be);
            this.dicomStatus = be.getDicomStatus();
        }

        public Integer getDicomStatus() {
            return dicomStatus;
        }

    }

}
