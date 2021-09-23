package org.bird.gateway.flags;


import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class Flags {


    /**
     * terminal UID
     */
    String clientUID = "";

    /**
     * online or Offline
     */
    Boolean online;

    /**
     * replace tag
     */
    Boolean tagsToReplace = false;

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

    /**
     * Archive storage address url.
     */
    String archiveUrl = "";

    /**
     * mpps address url.
     */
    String mppsUrl = "";

    /**
     * file upload retry。
     */
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
     * save mpps stream
     */
    String fileMpps = "";

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

    /**
     * Path to json containing aet definitions (array containing name/host/port per element)
     */
    String aetDictionaryPath = "";

    /**
     * Json array containing aet definitions (name/host/port per element). "
     * "Only one of aet_dictionary and aet_dictionary_inline needs to be specified."
     */
    String aetDictionaryInline = "";

    /**
     * upload retry amount
     */
    Integer persistentFileUploadRetryAmount = 0;

    /**
     * http codes list to retry that less than 500.
     */
    List<Integer> httpErrorCodesToRetry = new ArrayList<>(Arrays.asList(409));

    /**
     * minimum delay before upload backup file (ms)
     */
    Integer minUploadDelay = 3000;

    /**
     * maximum waiting time between uploads (ms)
     */
    Integer maxWaitingTimeBetweenUploads = 5000;

    /**
     * If true, when processing C-STORE requests with a destination config specified, the adapter will " +
     * "send to all matching destinations rather than the first matching destination.
     */
    Boolean sendToAllMatchingDestinations = false;

    /**
     * Path to json array containing destination definitions (filter/dicomweb_destination per element)
     */
    String destinationConfigPath = "";

    /**
     * Json array containing destination definitions (filter/dicomweb_destination per element). "
     * "Only one of destination_config_path and destination_config_inline needs to be specified.
     */
    String destinationConfigInline = "";

    /**
     * negotiate fuzzy semantic person name attribute matching. False by default.
     */
    Boolean fuzzyMatching = false;


}
