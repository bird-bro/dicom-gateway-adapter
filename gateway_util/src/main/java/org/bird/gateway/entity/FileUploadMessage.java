package org.bird.gateway.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author bird
 * @date 2021-7-2 13:33
 **/
@Data
public class FileUploadMessage implements Serializable  {

    private static final long serialVersionUID = 5352354L;
    private List<SuccessSopInfo> successSopInfoList;
    private List<WarningSopInfo> warningSopInfoList;
    private List<FailedSopInfo> failedSopInfoList;
    private List<String> archiveUrl;
    private String otherFailed;

}
