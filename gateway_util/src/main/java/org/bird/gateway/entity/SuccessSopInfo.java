package org.bird.gateway.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author bird
 * @date 2021-7-2 13:32
 **/
@Data
public class SuccessSopInfo implements Serializable {

    private static final long serialVersionUID = 5352355L;
    private List<String> referencedSOPClassUID;
    private List<String> referencedSOPInstanceUID;
    private List<String> retrieveURL;
    private String originalAttributesSequence;

}
