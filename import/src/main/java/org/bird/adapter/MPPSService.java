package org.bird.adapter;

import lombok.extern.slf4j.Slf4j;
import org.bird.adapter.cstore.destination.DestinationHolder;
import org.bird.adapter.cstore.destination.IDestinationClientFactory;
import org.bird.adapter.cstore.multipledest.IMultipleDestinationUploadService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.service.BasicMPPSSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.SafeClose;

import java.io.*;

@Slf4j
public class MPPSService extends BasicMPPSSCP {
    private final IDestinationClientFactory destinationClientFactory;
    private final IMultipleDestinationUploadService multipleSendService;
    private File storageDir;

    MPPSService(IDestinationClientFactory destinationClientFactory,
                String storeDirPath,
                IMultipleDestinationUploadService multipleSendService) {
        this.destinationClientFactory = destinationClientFactory;
        this.storageDir = new File(storeDirPath);
        this.multipleSendService = multipleSendService;
    }

    private void sendMpps(File file, String cmd, String aet) {
        try {
            InputStream in = new DicomInputStream(file);
            DestinationHolder destinationHolder = destinationClientFactory.create(aet, in);
            destinationHolder.getSingleDestination().sendMpps(in, cmd);
        } catch (Exception e) {
            log.trace("Error: ", e);
        }
    }

    @Override
    protected Attributes create(Association as, Attributes rq, Attributes rqAttrs, Attributes rsp) throws DicomServiceException {
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        File file = new File(storageDir, iuid);
        if (file.exists())
            throw new DicomServiceException(Status.DuplicateSOPinstance).
                    setUID(Tag.AffectedSOPInstanceUID, iuid);
        DicomOutputStream out = null;
        log.info("{}: M-WRITE {}", as, file);
        try {
            out = new DicomOutputStream(file);
            out.writeDataset(
                    Attributes.createFileMetaInformation(iuid, cuid,
                            UID.ExplicitVRLittleEndian),
                    rqAttrs);
        } catch (IOException e) {
            log.warn(as + ": Failed to store MPPS:", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        } finally {
            SafeClose.close(out);
        }

        sendMpps(file, "0", as.getCallingAET());

        return null;
    }

    @Override
    protected Attributes set(Association as, Attributes rq, Attributes rqAttrs, Attributes rsp) throws DicomServiceException {
        String cuid = rq.getString(Tag.RequestedSOPClassUID);
        String iuid = rq.getString(Tag.RequestedSOPInstanceUID);
        File file = new File(storageDir, iuid);
        if (!file.exists())
            throw new DicomServiceException(Status.NoSuchObjectInstance).
                    setUID(Tag.AffectedSOPInstanceUID, iuid);
        log.info("{}: M-UPDATE {}", as, file);
        Attributes data;
        DicomInputStream in = null;
        try {
            in = new DicomInputStream(file);
            data = in.readDataset(-1, -1);
        } catch (IOException e) {
            log.warn(as + ": Failed to read MPPS:", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        } finally {
            SafeClose.close(in);
        }
        if (!"IN PROGRESS".equals(data.getString(Tag.PerformedProcedureStepStatus)))
            BasicMPPSSCP.mayNoLongerBeUpdated();

        data.addAll(rqAttrs);
        DicomOutputStream out = null;
        try {
            out = new DicomOutputStream(file);
            out.writeDataset(
                    Attributes.createFileMetaInformation(iuid, cuid, UID.ExplicitVRLittleEndian),
                    data);
        } catch (IOException e) {
            log.warn(as + ": Failed to update MPPS:", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        } finally {
            SafeClose.close(out);
        }

        sendMpps(file, "1", as.getCallingAET());

        return null;
    }
}
