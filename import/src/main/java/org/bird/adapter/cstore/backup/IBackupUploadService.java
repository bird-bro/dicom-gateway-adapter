package org.bird.adapter.cstore.backup;

import org.bird.adapter.AetDictionary.Aet;
import org.bird.adapter.cstore.multipledest.sender.CStoreSender;
import org.bird.gateway.IGatewayClient;
import org.bird.adapter.cstore.backup.IBackupUploader.BackupException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * @author bird
 * @date 2021-7-2 16:43
 **/
public interface IBackupUploadService {

    void createBackup(InputStream inputStream, String uniqueFileName) throws BackupException;

    CompletableFuture startUploading(IGatewayClient webClient, BackupState backupState) throws BackupException;

    CompletableFuture startUploading(CStoreSender cStoreSender, Aet target, String sopInstanceUid, String sopClassUid,
                                     BackupState backupState) throws BackupException;

    void removeBackup(String uniqueFileName);


}
