package org.bird.adapter;

import lombok.extern.slf4j.Slf4j;
import org.bird.adapter.cmove.ISender;
import org.bird.adapter.cmove.ISenderFactory;
import org.bird.adapter.utils.AttributesUtils;
import org.bird.gateway.IGatewayClient;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCMoveSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.util.TagUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * @author bird
 * @date 2021-7-5 13:45
 **/
@Slf4j
public class CMoveService extends BasicCMoveSCP  {

    private final IGatewayClient gatewayClient;
    private final AetDictionary aets;
    private final ISenderFactory senderFactory;

    CMoveService(IGatewayClient gatewayClient, AetDictionary aets, ISenderFactory senderFactory) {
        super(UID.StudyRootQueryRetrieveInformationModelMOVE);
        this.gatewayClient = gatewayClient;
        this.aets = aets;
        this.senderFactory = senderFactory;
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes cmd, Attributes keys) throws IOException {

        if (dimse != Dimse.C_MOVE_RQ) {
            throw new DicomServiceException(Status.UnrecognizedOperation);
        }
        //MonitoringService.addEvent(Event.CMOVE_REQUEST);
        CMoveTask task = new CMoveTask(as, pc, cmd, keys);
        as.getApplicationEntity().getDevice().execute(task);
    }


    private class CMoveTask extends DimseTask {
        private final Attributes keys;

        private CMoveTask(Association as, PresentationContext pc, Attributes cmd, Attributes keys) {
            super(as, pc, cmd);

            this.keys = keys;
        }

        @Override
        public void run() {
            List<String> failedInstanceUids = new ArrayList<>();
            ISender sender = null;

            try {
                if (canceled) {
                    throw new CancellationException();
                }
                runThread = Thread.currentThread();

                AetDictionary.Aet cstoreTarget = aets.getAet(cmd.getString(Tag.MoveDestination));
                if (cstoreTarget == null) {
                    sendErrorResponse(Status.MoveDestinationUnknown,
                            "Unknown AET: " + cmd.getString(Tag.MoveDestination));
                    return;
                }

                // need to get instances belonging to series/study
                Attributes keysCopy = new Attributes(keys);
                keysCopy.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
                String qidoPath;
                try {
                    qidoPath = AttributesUtils.attributesToQidoPath(keysCopy);
                    log.info("CMove QidoPath: " + qidoPath);
                } catch (DicomServiceException e) {
                    log.error("CMove QidoPath error");
                    sendErrorResponse(e.getStatus(), e.getMessage(), null);
                    return;
                }

                JSONArray qidoResult;

                try {
                    //MonitoringService.addEvent(Event.CMOVE_QIDORS_REQUEST);
                    qidoResult = gatewayClient.qidoRs(qidoPath);
                    if (qidoResult == null || qidoResult.length() == 0) {
                        throw new IGatewayClient.DicomGatewayException("No instances to move",
                                Status.UnableToCalculateNumberOfMatches);
                    }
                } catch (IGatewayClient.DicomGatewayException e) {
                    //MonitoringService.addEvent(Event.CMOVE_QIDORS_ERROR);
                    log.error("CMove Qido-rs error", e);
                    sendErrorResponse(e.getStatus(), e.getMessage());
                    return;
                }

                sender = senderFactory.create();
                int successfullInstances = 0;
                int remainingInstances = qidoResult.length();

                for (Object instance : qidoResult) {
                    sendPendingResponse(remainingInstances, successfullInstances, failedInstanceUids.size());
                    if (canceled) {
                        throw new CancellationException();
                    }
                    JSONObject instanceJson = (JSONObject) instance;
                    String studyUid = AttributesUtils.getTagValue(instanceJson,
                            TagUtils.toHexString(Tag.StudyInstanceUID));
                    String seriesUid = AttributesUtils.getTagValue(instanceJson,
                            TagUtils.toHexString(Tag.SeriesInstanceUID));
                    String instanceUid = AttributesUtils.getTagValue(instanceJson,
                            TagUtils.toHexString(Tag.SOPInstanceUID));
                    String classUid = AttributesUtils.getTagValue(instanceJson,
                            TagUtils.toHexString(Tag.SOPClassUID));
                    try {
                        //MonitoringService.addEvent(Event.CMOVE_CSTORE_REQUEST);
                        long bytesSent = sender.cmove(cstoreTarget, studyUid, seriesUid,
                                instanceUid, classUid);
                        successfullInstances++;
                        //MonitoringService.addEvent(Event.CMOVE_CSTORE_BYTES, bytesSent);
                    } catch (IGatewayClient.DicomGatewayException | IOException e) {
                        //MonitoringService.addEvent(Event.CMOVE_CSTORE_ERROR);
                        log.error("Failed CStore within CMove", e);
                        failedInstanceUids.add(instanceUid);
                    }
                    remainingInstances--;
                }

                if (failedInstanceUids.isEmpty()) {
                    as.tryWriteDimseRSP(pc, Commands.mkCMoveRSP(cmd, Status.Success));
                } else {
                    int status = successfullInstances > 0 ?
                            Status.OneOrMoreFailures : Status.UnableToPerformSubOperations;
                    sendErrorResponse(status, failedInstanceUids);
                }

            }catch (CancellationException | InterruptedException e) {
                log.info("Canceled CMove", e);
                sendErrorResponse(Status.Cancel, failedInstanceUids);
            } catch (Throwable e) {
                log.error("Failure processing CMove", e);
                sendErrorResponse(Status.ProcessingFailure, e.getMessage());
            } finally {
                synchronized (this) {
                    runThread = null;
                }
                int msgId = cmd.getInt(Tag.MessageID, -1);
                as.removeCancelRQHandler(msgId);

                if (sender != null) {
                    try {
                        sender.close();
                    } catch (IOException e) {
                        log.error("Failure closing cstoreSender: ", e);
                    }
                }
            }
        }

        private void sendErrorResponse(int status, String message) {
            sendErrorResponse(status, message, null);
        }

        private void sendErrorResponse(int status, List<String> failedInstanceUids) {
            sendErrorResponse(status, null, failedInstanceUids);
        }

        // It seems WEASIS (at least, GINKGO/AESKULAP don't cancel at all) doesn't just send cancel-rq,
        // when it wants to cancel.
        // Instead it replies with cancel-rq to any move-rsp, including pending
        // (which I can spam as much as I want).
        // Which while not contradicting the standard, is weird (Pending responses are optional).
        private void sendPendingResponse(
                int remainingInstances,
                int successfullInstances,
                int failedInstances)
                throws CancellationException {
            Attributes attributes = new Attributes();
            attributes.setInt(Tag.NumberOfRemainingSuboperations, VR.US, remainingInstances);
            attributes.setInt(Tag.NumberOfCompletedSuboperations, VR.US, successfullInstances);
            attributes.setInt(Tag.NumberOfFailedSuboperations, VR.US, failedInstances);
            // no code path for warnings
            attributes.setInt(Tag.NumberOfWarningSuboperations, VR.US, 0);
            as.tryWriteDimseRSP(pc, Commands.mkCMoveRSP(cmd, Status.Pending), attributes);
        }

        private void sendErrorResponse(int status, String message, List<String> failedInstanceUids) {
            switch (status) {
                case Status.Cancel:
                    //MonitoringService.addEvent(Event.CMOVE_CANCEL);
                    break;
                case Status.OneOrMoreFailures:
                    //MonitoringService.addEvent(Event.CMOVE_WARNING);
                    break;
                default:
                    //MonitoringService.addEvent(Event.CMOVE_ERROR);
            }

            Attributes cmdAttr = Commands.mkCMoveRSP(cmd, status);
            if (message != null) {
                cmdAttr.setString(Tag.ErrorComment, VR.LO, message);
            }

            if (failedInstanceUids != null && failedInstanceUids.size() > 0) {
                Attributes dataAttr = new Attributes();
                dataAttr.setString(Tag.FailedSOPInstanceUIDList, VR.UI,
                        failedInstanceUids.toArray(new String[]{}));
                as.tryWriteDimseRSP(pc, cmdAttr, dataAttr);
            } else {
                as.tryWriteDimseRSP(pc, cmdAttr);
            }
        }
    }

}
