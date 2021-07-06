package org.bird.gateway.enums;

/**
 * @author bird
 * @date 2021-7-2 16:19
 **/
public enum ArchiveInfoEnum {

    SUCCESS_CODE(200,"文件已经上传,DCM服务器接受并解析入库成功!"),
    ERROR_CODE_201(201,"文件已经完整上传,等待DCM服务器返回中!"),
    ERROR_CODE_202(202,"上传成功!"),
    ERROR_CODE_203(203,"文件已经上传,DCM服务器接受并解析文件失败!"),
    ERROR_CODE_500(500,"系统异常!请重新上传!"),
    ERROR_CODE_501(501,"系统异常!不可以再次上传!"),
    ERROR_CODE_509(509,"文件组合情况超出正常情况!"),
    ERROR_CODE_502(502,"检测文件是否需要合并异常!"),
    ERROR_CODE_503(503,"文件片段已经上传!"),
    ERROR_CODE_504(504,"正文文件合成失败!"),
    ERROR_CODE_505(505,"入参参数错误!"),
    ERROR_CODE_506(506,"文件HASH值校验失败!"),
    ERROR_CODE_507(507,"服务器连接超时!"),
    ERROR_CODE_508(508,"上传文件被DICOM服务器检测不规范!"),
    ERROR_CODE_517(517,"上传DICOM服务器请求成功,但是业务失败!"),
    ERROR_CODE_518(518,"上传文件初始化失败!"),
    ERROR_CODE_519(519,"文件正在被占用中!");

    private int code;
    private String message;

    private ArchiveInfoEnum(int code , String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
