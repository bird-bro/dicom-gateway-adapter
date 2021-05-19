package org.bird.store.config;


import lombok.Data;

@Data
public class Flags {

    /**
     * terminal UID
     */
    private String clientUID = "";

    /**
     * Title of DIMSE Application Entity.
     */
    String dimseAET = "";
    /**
     * Port the server is listening to for incoming DIMSE requests.
     */
    Integer dimsePort = 0;

    /**
     * Address for Dicom Gateway service.
     */
    String gatewayAddress = "";

    String gatewayUrl = "";

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
     * (Optional) Separate AET used for C-STORE calls within context of C-MOVE.
     */
    String dimseCmoveAET = "";

    /**
     * Whether to use HTTP 2.0 for StowRS (i.e. StoreInstances) requests. True by default.
     */
    Boolean useHttp2ForStow = false;


    /**
     * Tags to remove during C-STORE upload, comma separated. Only one of 'redact' flags may be present
     */
    String tagsToRemove = "00100020";
    Boolean tagsToReplace = false;

    /**
     * Tags to keep during C-STORE upload, comma separated. Only one of 'redact' flags may be present
     */
    String tagsToKeep = "";

    /**
     * Filter tags by predefined profile during C-STORE upload. Only one of 'redact' flags may be present. Values: CHC_BASIC"
     */
    String tagsProfile = "";

}
