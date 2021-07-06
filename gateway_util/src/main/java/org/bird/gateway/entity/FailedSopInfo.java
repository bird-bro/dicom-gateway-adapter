package org.bird.gateway.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author bird
 * @date 2021-7-2 13:33
 **/
@Data
public class FailedSopInfo implements Serializable  {

    private static final long serialVersionUID = 5362354L;
    private List<String> referencedSOPClassUID;
    private List<String> referencedSOPInstanceUID;
    private String failureReason;

}
