package org.bird.adapter;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.cloud.healthcare.deid.redactor.protos.DicomConfigProtos.DicomConfig;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.bird.adapter.cmove.CMoveSenderFactory;
import org.bird.adapter.cstore.backup.BackupUploadService;
import org.bird.adapter.cstore.backup.DelayCalculator;
import org.bird.adapter.cstore.backup.IBackupUploader;
import org.bird.adapter.cstore.backup.LocalBackupUploader;
import org.bird.adapter.cstore.destination.IDestinationClientFactory;
import org.bird.adapter.cstore.destination.MultipleDestinationClientFactory;
import org.bird.adapter.cstore.destination.SingleDestinationClientFactory;
import org.bird.adapter.cstore.multipledest.MultipleDestinationUploadService;
import org.bird.adapter.cstore.multipledest.sender.CStoreSenderFactory;
import org.bird.adapter.utils.DeviceUtils;
import org.bird.adapter.utils.JsonUtils;
import org.bird.gateway.*;
import org.bird.gateway.flags.Flags;
import org.bird.gateway.flags.Configurator;
import org.bird.gateway.utils.GatewayUtils;
import org.bird.gateway.utils.StringUtils;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.bird.adapter.AetDictionary.Aet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class ImportAdapter {

    private static final String STUDIES = "studies";
    private static final String FILTER = "filter";


    public static void main(String[] args) throws IOException, GeneralSecurityException {
        Flags flags = Configurator.configurator();
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

        // Dicom service handlers
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        // Handle C-ECHO (all nodes which accept associations must support this)
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        // Handle C-STORE
        String cstoreAddr = flags.getArchiveAddress();
        String cstorePath = STUDIES;
        String cstoreSubAet = flags.getDimseAET();

        if (cstoreSubAet == null || cstoreSubAet.isBlank()) {
            throw new IllegalArgumentException("-- dimse_aet flag must be set.");
        }

        IGatewayClient defaultCstoreGatewayClient = configureDefaultGatewayClient(requestFactory, cstoreAddr, cstorePath, flags);

        DicomRedactor redactor = configureRedactor(flags);

        BackupUploadService backupUploadService = configureBackupUploadService(flags);


        IDestinationClientFactory destinationClientFactory = configureDestinationClientFactory(
                defaultCstoreGatewayClient, flags, backupUploadService != null);


        MultipleDestinationUploadService multipleDestinationSendService = configureMultipleDestinationUploadService(
                flags, cstoreSubAet, backupUploadService);


        CStoreService cStoreService =
                new CStoreService(destinationClientFactory, redactor, flags.getTranscodeToSyntax(), multipleDestinationSendService);
        serviceRegistry.addDicomService(cStoreService);


        // Handle C-FIND
        IGatewayClient dicomWebClient = new GatewayClient(requestFactory, flags.getArchiveAddress(), STUDIES);
        CFindService cFindService = new CFindService(dicomWebClient, flags);
        serviceRegistry.addDicomService(cFindService);

        // Handle C-MOVE
        CMoveSenderFactory cMoveSenderFactory = new CMoveSenderFactory(cstoreSubAet, dicomWebClient);
        AetDictionary aetDict = new AetDictionary(flags.getAetDictionaryInline(), flags.getAetDictionaryPath());
        CMoveService cMoveService = new CMoveService(dicomWebClient, aetDict, cMoveSenderFactory);
        serviceRegistry.addDicomService(cMoveService);

        // Handle Storage Commitment N-ACTION
        serviceRegistry.addDicomService(new StorageCommitmentService(dicomWebClient, aetDict));

        // Start DICOM server
        Device device = DeviceUtils.createServerDevice(flags.getDimseAET(), flags.getDimsePort(), serviceRegistry);
        device.bindConnections();


        RetryService.getInstance(new RetryService.TaskProcessor() {
            @Override
            public void process(String filePath) throws Exception {
                log.info(filePath);
                RetryStoreService.getInstance(defaultCstoreGatewayClient).stowRs(filePath);
            }
        });
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                GatewayUtils.postHeartbeat();
            }
        },1000L, 60000L);
    }





    private static MultipleDestinationUploadService configureMultipleDestinationUploadService(
            Flags flags,
            String cstoreSubAet,
            BackupUploadService backupUploadService) {
        if (backupUploadService != null) {
            return new MultipleDestinationUploadService(
                    new CStoreSenderFactory(cstoreSubAet),
                    backupUploadService,
                    flags.getPersistentFileUploadRetryAmount());
        }
        return null;
    }



    private static IGatewayClient configureDefaultGatewayClient(
            HttpRequestFactory requestFactory,
            String cstoreDicomWebAddr,
            String cstoreDicomWebStowPath,
            Flags flags){
        IGatewayClient defaultCstoreGatewayClient;
        if(flags.getUseHttp2ForStow()){
            defaultCstoreGatewayClient = new GatewayClientJetty(StringUtils.joinPath(cstoreDicomWebAddr, cstoreDicomWebStowPath));
        }else {
            defaultCstoreGatewayClient = new GatewayClient(requestFactory, cstoreDicomWebAddr, cstoreDicomWebStowPath);
        }
        return defaultCstoreGatewayClient;
    }



    private static DicomRedactor configureRedactor(Flags flags) throws IOException{
        DicomRedactor redactor = null;
        int tagEditFlags =
                        (flags.getTagsToRemove().isEmpty() ? 0 : 1) +
                        (flags.getTagsToKeep().isEmpty() ? 0 : 1) +
                        (flags.getTagsProfile().isEmpty() ? 0 : 1);
        if (tagEditFlags > 1) {
            throw new IllegalArgumentException("Only one of 'redact' flags may be present");
        }
        if (tagEditFlags > 0) {
            DicomConfig.Builder configBuilder = DicomConfig.newBuilder();
            if (!flags.getTagsToRemove().isEmpty()) {
                List<String> removeList = Arrays.asList(flags.getTagsToRemove().split(","));
                configBuilder.setRemoveList(
                        DicomConfig.TagFilterList.newBuilder().addAllTags(removeList));
            }else if (!flags.getTagsToKeep().isEmpty()) {
                List<String> keepList = Arrays.asList(flags.getTagsToKeep().split(","));
                configBuilder.setKeepList(
                        DicomConfig.TagFilterList.newBuilder().addAllTags(keepList));
            } else if (!flags.getTagsProfile().isEmpty()){
                configBuilder.setFilterProfile(DicomConfig.TagFilterProfile.valueOf(flags.getTagsProfile()));
            }
            try {
                redactor = new DicomRedactor(configBuilder.build(),flags.getClientUID(),flags.getTagsToReplace());
            } catch (Exception e) {
                throw new IOException("Failure creating DICOM redactor", e);
            }
        }
        return redactor;
    }


    private static BackupUploadService configureBackupUploadService(Flags flags) throws IOException {
        String uploadPath = flags.getFileUploadCache();

        if (!uploadPath.isBlank()) {
            final IBackupUploader backupUploader;

            backupUploader = new LocalBackupUploader(uploadPath);
            return new BackupUploadService(
                    backupUploader,
                    flags.getFileUploadRetry(),
                    ImmutableList.copyOf(flags.getHttpErrorCodesToRetry()),
                    new DelayCalculator(flags.getMinUploadDelay(), flags.getMaxWaitingTimeBetweenUploads()));
        }
        return null;
    }


    private static IDestinationClientFactory configureDestinationClientFactory(
            IGatewayClient defaultCstoreGatewayClient, Flags flags, boolean backupServicePresent) throws IOException {

        IDestinationClientFactory destinationClientFactory;
        if (flags.getSendToAllMatchingDestinations()) {
            if (backupServicePresent == false) {
                throw new IllegalArgumentException("backup is not configured properly. '--send_to_all_matching_destinations' " +
                        "flag must be used only in pair with backup, local or GCP. Please see readme to configure backup.");
            }
            Pair<ImmutableList<Pair<DestinationFilter, IGatewayClient>>,
                    ImmutableList<Pair<DestinationFilter, Aet>>> multipleDestinations = configureMultipleDestinationTypesMap(
                    flags.getDestinationConfigInline(),
                    flags.getDestinationConfigPath(),
                    DestinationsConfig.ENV_DESTINATION_CONFIG_JSON);
            destinationClientFactory = new MultipleDestinationClientFactory(
                    multipleDestinations.getLeft(),
                    multipleDestinations.getRight(),
                    defaultCstoreGatewayClient);
        }else {
            // with or without backup usage.
            destinationClientFactory = new SingleDestinationClientFactory(
                    configureDestinationMap(flags.getDestinationConfigInline(), flags.getDestinationConfigPath()), defaultCstoreGatewayClient);
        }
        return destinationClientFactory;
    }

    private static ImmutableList<Pair<DestinationFilter, IGatewayClient>> configureDestinationMap(
            String destinationJsonInline,
            String destinationsJsonPath) throws IOException {
        DestinationsConfig conf = new DestinationsConfig(destinationJsonInline, destinationsJsonPath);
        ImmutableList.Builder<Pair<DestinationFilter, IGatewayClient>> filterPairBuilder = ImmutableList.builder();
        for (String filterString : conf.getMap().keySet()) {
            String filterPath = StringUtils.trim(conf.getMap().get(filterString));
            filterPairBuilder.add(
                    new Pair(
                            new DestinationFilter(filterString),
                            new GatewayClientJetty(filterPath.endsWith(STUDIES)? filterPath : StringUtils.joinPath(filterPath, STUDIES))
                    ));
        }
        ImmutableList resultList = filterPairBuilder.build();
        return resultList.size() > 0 ? resultList : null;
    }

    public static Pair<ImmutableList<Pair<DestinationFilter, IGatewayClient>>,
            ImmutableList<Pair<DestinationFilter, Aet>>> configureMultipleDestinationTypesMap(
            String destinationJsonInline,
            String jsonPath,
            String jsonEnvKey) throws IOException{

        ImmutableList.Builder<Pair<DestinationFilter, Aet>> dicomDestinationFiltersBuilder = ImmutableList.builder();
        ImmutableList.Builder<Pair<DestinationFilter, IGatewayClient>> healthDestinationFiltersBuilder = ImmutableList.builder();
        JSONArray jsonArray = JsonUtils.parseConfig(destinationJsonInline, jsonPath, jsonEnvKey);
        if (jsonArray != null) {
            for (Object elem : jsonArray) {
                JSONObject elemJson = (JSONObject) elem;
                if (elemJson.has(FILTER) == false) {
                    throw new IOException("Mandatory key absent: " + FILTER);
                }
                String filter = elemJson.getString(FILTER);
                DestinationFilter destinationFilter = new DestinationFilter(StringUtils.trim(filter));

                // try to create Aet instance
                if (elemJson.has("host")) {
                    dicomDestinationFiltersBuilder.add(
                            new Pair(destinationFilter,
                                    new Aet(elemJson.getString("name"),
                                            elemJson.getString("host"), elemJson.getInt("port"))));
                } else {
                    // in this case to try create IDicomWebClient instance
                    String filterPath = elemJson.getString("dicomweb_destination");
                    healthDestinationFiltersBuilder.add(
                            new Pair(
                                    destinationFilter,
                                    new GatewayClientJetty(filterPath.endsWith(STUDIES)? filterPath : StringUtils.joinPath(filterPath, STUDIES))));
                }
            }
        }
        return new Pair(healthDestinationFiltersBuilder.build(), dicomDestinationFiltersBuilder.build());
    }









    public static class Pair<A, D>{
        private final A left;
        private final D right;

        public Pair(A left, D right) {
            this.left = left;
            this.right = right;
        }

        public A getLeft() {
            return left;
        }

        public D getRight() {
            return right;
        }
    }




}
