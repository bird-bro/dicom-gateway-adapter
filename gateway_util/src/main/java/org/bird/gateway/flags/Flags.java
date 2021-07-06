package org.bird.gateway.flags;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Flags {

    /**
     * Address for Dicom Gateway service.
     */
    String gatewayApi = "";
    String gatewayApiOauth = "";
    String clientAk = "";
    String clientSk = "";
    String clientEnv = "";


    /**
     * terminal UID
     */
    String clientUID = "";

    /**
     * Title of DIMSE Application Entity.
     */
    String dimseAET = "";
    /**
     * Port the server is listening to for incoming DIMSE requests.
     */
    Integer dimsePort = 11112;

    /**
     * Archive storage address.
     */
    String archiveAddress = "";

    String archiveUrl = "";









    String fileRetry="";
    /**
     * temporary location for storing files before send
     */
    String fileUploadCache = "";

    /**
     * upload retry amount
     */
    Integer fileUploadRetry = 0;

    /**
     * Transfer Syntax to convert instances to during C-STORE upload. See Readme for list of supported syntaxes.
     */
    String transcodeToSyntax = "";

    /**
     * Prints out debug messages.
     */
    boolean verbose = true;
    /**
     * Whether to use HTTP 2.0 for StowRS (i.e. StoreInstances) requests. True by default.
     */
    Boolean useHttp2ForStow = false;

    Boolean tagsToReplace = false;




    /**
     * (Optional) Separate AET used for C-STORE calls within context of C-MOVE.
     */
    String dimseCmoveAET = "";

    /**
     * Tags to remove during C-STORE upload, comma separated. Only one of 'redact' flags may be present
     */
    String tagsToRemove = "00100020";

    /**
     * Tags to keep during C-STORE upload, comma separated. Only one of 'redact' flags may be present
     */
    String tagsToKeep = "";

    /**
     * Filter tags by predefined profile during C-STORE upload. Only one of 'redact' flags may be present. Values: CHC_BASIC"
     */
    String tagsProfile = "";

    String aetDictionaryPath = "";

    String aetDictionaryInline = "";

    /**
     * upload retry amount
     */
    Integer persistentFileUploadRetryAmount = 0;


    List<Integer> httpErrorCodesToRetry = new ArrayList<>();

    Integer minUploadDelay = 10;

    Integer maxWaitingTimeBetweenUploads = 10;

    Boolean sendToAllMatchingDestinations = false;

    String destinationConfigPath = "";

    String destinationConfigInline = "";

    Boolean fuzzyMatching = false;


}
