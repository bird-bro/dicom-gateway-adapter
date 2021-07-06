package org.bird.adapter.cstore.backup;

/**
 * @author bird
 * @date 2021-7-2 16:54
 **/
public abstract class AbstractBackupUploader implements IBackupUploader {

    private String uploadFilePath;

    public AbstractBackupUploader(String uploadFilePath) {
        this.uploadFilePath = uploadFilePath;
    }

    public String getUploadFilePath() {
        return uploadFilePath;
    }

    public void validatePathParameter(String parameterValue, String parameterName) throws BackupException {
        if (parameterValue == null || parameterValue.isBlank()) {
            throw new BackupException("Invalid upload path, parameter - " + parameterName + " is blank.");
        }
    }

}
