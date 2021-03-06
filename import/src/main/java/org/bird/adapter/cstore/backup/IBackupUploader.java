package org.bird.adapter.cstore.backup;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author bird
 * @date 2021-7-2 16:53
 **/
public interface IBackupUploader {

    void doWriteBackup(InputStream inputStream, String uniqueFileName) throws BackupException;

    InputStream doReadBackup(String uniqueFileName) throws BackupException;

    void doRemoveBackup(String uniqueFileName) throws BackupException;




    class BackupException extends IOException {

        private Integer dicomStatus;

        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }

        public BackupException(int dicomStatus, Throwable cause, String message) {
            super(message, cause);
            this.dicomStatus = dicomStatus;
        }

        public BackupException(String message) {
            super(message);
        }

        public BackupException(Throwable cause) {
            super(cause);
        }

        public BackupException(int dicomStatus, String message) {
            super(message);
            this.dicomStatus = dicomStatus;
        }

        public Integer getDicomStatus() {
            return dicomStatus;
        }
    }

}
