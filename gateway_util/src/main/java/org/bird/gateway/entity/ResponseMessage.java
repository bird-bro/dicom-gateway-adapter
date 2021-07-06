package org.bird.gateway.entity;

import lombok.Data;

import java.util.HashMap;

/**
 * @author bird
 * @date 2021-7-2 13:29
 **/
@Data
public class ResponseMessage {

    private int code;

    private String message;

    private String userId;

    HashMap<String, Object> uploadMessage;

}
