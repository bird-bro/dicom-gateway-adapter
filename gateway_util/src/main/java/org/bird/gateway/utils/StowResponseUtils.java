package org.bird.gateway.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bird.adapter.utils.WordUtils;
import org.bird.gateway.entity.*;
import org.bird.gateway.enums.ArchiveInfoEnum;
import org.bird.gateway.enums.ClientInfoEnum;
import org.bird.gateway.flags.Configurator;
import org.bird.gateway.flags.Flags;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author bird
 * @date 2021-7-2 16:16
 **/
@Slf4j
public class StowResponseUtils {
    private static final Flags FLAGS = Configurator.Configurator();
    boolean retry = false;
    public final int code;
    public final InputStream stream;
    private ResponseMessage responseMessage = new ResponseMessage();

    public StowResponseUtils(int code, InputStream stream) throws IOException {
        this.code = code;
        this.stream = stream;
    }


    public ArchiveInfoEnum responseInfo() throws Exception {
        FileUploadMessage uploadMessage = new FileUploadMessage();
        BufferedReader reader = null;
        String line = null;
        StringBuffer jsonResult = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                jsonResult.append(line);
            }
            log.info("--Archive STOW_RS response json:{}", jsonResult.toString());
            return analyzeResultJson(jsonResult.toString(), uploadMessage);
        } catch (Exception e) {
            throw e;
        }
    }

    public ResponseMessage responseMessage() throws Exception {

        FileUploadMessage uploadMessage = new FileUploadMessage();
        BufferedReader reader = null;
        String line = null;
        StringBuffer jsonResult = new StringBuffer();

        try {
            reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                jsonResult.append(line);
            }
            log.info("调用Archive-STOW_RS,return json:{}", jsonResult.toString());

            ArchiveInfoEnum infoEnum = (analyzeResultJson(jsonResult.toString(), uploadMessage));
            responseMessage.setCode(infoEnum.getCode());
            responseMessage.setMessage(infoEnum.getMessage());
            responseMessage.setUploadMessage(getMessageMap(uploadMessage));
            return responseMessage;
        } catch (Exception e) {
            throw e;
        }
    }


    public ArchiveInfoEnum analyzeResultJson(String jsonResult, FileUploadMessage uploadMessage) {
        jsonResult = StringUtils.isBlank(jsonResult) ? "{}" : jsonResult;
        HashMap joResult = JSONObject.parseObject(jsonResult, HashMap.class);

        List<SuccessSopInfo> successSopInfoList = new ArrayList<SuccessSopInfo>();
        List<WarningSopInfo> warningSopInfoList = new ArrayList<WarningSopInfo>();
        List<FailedSopInfo> failedSopInfoList = new ArrayList<FailedSopInfo>();
        List<String> archiveUrl = new ArrayList<String>();

        // 获取成功信息
        String str99 = "";
        if (joResult.get("00081199") != null) {
            str99 = joResult.get("00081199").toString();
        }
        // 获取失败信息
        String str98 = "";
        if (joResult.get("00081198") != null) {
            str98 = joResult.get("00081198").toString();
        }
        // 获取其他失败信息
        String str9A = "";
        if (joResult.get("0008119A") != null) {
            str9A = joResult.get("0008119A").toString();
        }
        // 获取路径信息
        String str90 = "";
        if (joResult.get("00081190") != null) {
            str90 = joResult.get("00081190").toString();
        }

        if (StringUtils.isNotBlank(str90) && str90.startsWith("{") && str90.endsWith("}")) {
            dealWith90(archiveUrl, str90);
            uploadMessage.setArchiveUrl(archiveUrl);
        }
        if (StringUtils.isNotBlank(str99) && str99.startsWith("{") && str99.endsWith("}")) {
            dealWithStr99(successSopInfoList, warningSopInfoList, str99);
            if (successSopInfoList.size() != 0) {
                uploadMessage.setSuccessSopInfoList(successSopInfoList);
            }
            if (warningSopInfoList.size() != 0) {
                uploadMessage.setWarningSopInfoList(warningSopInfoList);
            }
        }
        if (StringUtils.isNotBlank(str98) && str98.startsWith("{") && str98.endsWith("}")) {
            dealWithStr98(failedSopInfoList, str98);
            uploadMessage.setFailedSopInfoList(failedSopInfoList);
        }
        if (StringUtils.isNotBlank(str9A)) {
            dealWithStr9A(str9A);
            uploadMessage.setOtherFailed(str9A);
        }

        if ((uploadMessage.getFailedSopInfoList() == null || uploadMessage.getFailedSopInfoList().size() == 0)
                && (uploadMessage.getSuccessSopInfoList() == null || uploadMessage.getSuccessSopInfoList().size() == 0)) {
            return ArchiveInfoEnum.ERROR_CODE_203;
        } else if ((uploadMessage.getFailedSopInfoList() == null || uploadMessage.getFailedSopInfoList().size() == 0)
                && uploadMessage.getSuccessSopInfoList() != null && uploadMessage.getSuccessSopInfoList().size() > 0) {
            return ArchiveInfoEnum.SUCCESS_CODE;
        } else if (uploadMessage.getFailedSopInfoList() != null && uploadMessage.getFailedSopInfoList().size() > 0 && retry) {
            return ArchiveInfoEnum.ERROR_CODE_500;
        } else if (uploadMessage.getFailedSopInfoList() != null && uploadMessage.getFailedSopInfoList().size() > 0 && !retry) {
            return ArchiveInfoEnum.ERROR_CODE_203;
        } else {
            return ArchiveInfoEnum.SUCCESS_CODE;
        }
    }


    private void dealWithStr9A(String str9A) {
        HashMap map9A = JSONObject.parseObject(str9A, HashMap.class);
        String map9AValue = "";
        if (map9A.get("Value") != null) {
            map9AValue = map9A.get("Value").toString();
            if (StringUtils.isNotBlank(map9AValue) && map9AValue.startsWith("[") && map9AValue.endsWith("]")) {
                map9AValue = map9AValue.substring(1, map9AValue.length() - 1);
            }
            if (StringUtils.isNotBlank(map9AValue) && StringUtils.isNumeric(map9AValue)) {
                int parseValue = Integer.parseInt(map9AValue);
                if (parseValue == 272 || (42752 <= parseValue && parseValue <= 43007)) {
                    retry = true;
                }
            }
        }
    }

    public void dealWith90 (List<String> archiveUrl, String str90){
        HashMap map90 = JSONObject.parseObject(str90, HashMap.class);
        String map90Value = "";
        if (map90.get("Value") != null) {
            map90Value = map90.get("Value").toString();
        }
        if (StringUtils.isNotBlank(map90Value) && map90Value.startsWith("[") && map90Value.endsWith("]")) {
            archiveUrl.add(map90Value);
        }
    }

    public void dealWithStr98(List<FailedSopInfo> failedSopInfoList, String str98) {
        HashMap map98 = JSONObject.parseObject(str98, HashMap.class);
        String map98Value = "";
        if (map98.get("Value") != null) {
            map98Value = map98.get("Value").toString();
        }
        if (StringUtils.isNotBlank(map98Value) && map98Value.startsWith("[") && map98Value.endsWith("]")) {
            ArrayList map98ValueList = JSONObject.parseObject(map98Value, ArrayList.class);
            for (int i = 0; i < map98ValueList.size(); i++) {
                String map98ValueListn = "";
                if (map98ValueList.get(i) != null) {
                    map98ValueListn = map98ValueList.get(i).toString();
                }
                if (StringUtils.isNotBlank(map98ValueListn) && map98ValueListn.startsWith("{")
                        && map98ValueListn.endsWith("}")) {
                    FailedSopInfo fn = new FailedSopInfo();
                    HashMap map98ValueListnMap = JSONObject.parseObject(map98ValueListn, HashMap.class);
                    String map98_50 = "";
                    if (map98ValueListnMap.get("00081150") != null) {
                        map98_50 = map98ValueListnMap.get("00081150").toString();
                    }
                    String map98_55 = "";
                    if (map98ValueListnMap.get("00081155") != null) {
                        map98_55 = map98ValueListnMap.get("00081155").toString();
                    }
                    String map98_97 = "";
                    if (map98ValueListnMap.get("00081197") != null) {
                        map98_97 = map98ValueListnMap.get("00081197").toString();
                    }

                    if (StringUtils.isNotBlank(map98_50) && map98_50.startsWith("{") && map98_50.endsWith("}")) {
                        HashMap map98_50Map = JSONObject.parseObject(map98_50, HashMap.class);
                        String map98_50MapValue = "";
                        if (map98_50Map.get("Value") != null) {
                            map98_50MapValue = map98_50Map.get("Value").toString();
                        }
                        if (StringUtils.isNotBlank(map98_50MapValue) && map98_50MapValue.startsWith("[")
                                && map98_50MapValue.endsWith("]")) {
                            ArrayList map98_50MapValueList = JSONObject.parseObject(map98_50MapValue, ArrayList.class);
                            fn.setReferencedSOPClassUID(map98_50MapValueList);
                        }
                    }
                    if (StringUtils.isNotBlank(map98_55) && map98_55.startsWith("{") && map98_55.endsWith("}")) {
                        HashMap map98_55Map = JSONObject.parseObject(map98_55, HashMap.class);
                        String map98_55MapValue = "";
                        if (map98_55Map.get("Value") != null) {
                            map98_55MapValue = map98_55Map.get("Value").toString();
                        }
                        if (StringUtils.isNotBlank(map98_55MapValue) && map98_55MapValue.startsWith("[")
                                && map98_55MapValue.endsWith("]")) {
                            ArrayList map99_55MapValueList = JSONObject.parseObject(map98_55MapValue, ArrayList.class);
                            fn.setReferencedSOPInstanceUID(map99_55MapValueList);
                        }
                    }
                    if (StringUtils.isNotBlank(map98_97) && map98_97.startsWith("{") && map98_97.endsWith("}")) {
                        HashMap map98_97Map = JSONObject.parseObject(map98_97, HashMap.class);
                        String map98_97MapValue = "";
                        if (map98_97Map.get("Value") != null) {
                            map98_97MapValue = map98_97Map.get("Value").toString();
                            if(StringUtils.isNotBlank(map98_97MapValue)&&map98_97MapValue.startsWith("[")&&map98_97MapValue.endsWith("]")){
                                map98_97MapValue=map98_97MapValue.substring(1,map98_97MapValue.length()-1);
                            }
                            if (StringUtils.isNotBlank(map98_97MapValue) && StringUtils.isNumeric(map98_97MapValue)) {
                                int parseValue = Integer.parseInt(map98_97MapValue);
                                if (parseValue == 272 || (42752 <= parseValue && parseValue <= 43007 && parseValue != 42872)) {
                                    retry = true;
                                }
                            }
                        }
                        fn.setFailureReason(map98_97MapValue);
                    }
                    failedSopInfoList.add(fn);
                }
            }
        }
    }

    public void dealWithStr99(List<SuccessSopInfo> successSopInfoList, List<WarningSopInfo> warningSopInfoList, String str99) {
        HashMap map99 = JSONObject.parseObject(str99, HashMap.class);
        String map99Value = "";
        if (map99.get("Value") != null) {
            map99Value = map99.get("Value").toString();
        }
        if (StringUtils.isNotBlank(map99Value) && map99Value.startsWith("[") && map99Value.endsWith("]")) {
            ArrayList map99ValueList = JSONObject.parseObject(map99Value, ArrayList.class);
            for (int i = 0; i < map99ValueList.size(); i++) {
                String map99ValueListn = "";
                if (map99ValueList.get(i) != null) {
                    map99ValueListn = map99ValueList.get(i).toString();
                }
                if (StringUtils.isNotBlank(map99ValueListn) && map99ValueListn.startsWith("{")
                        && map99ValueListn.endsWith("}")) {
                    SuccessSopInfo sn = new SuccessSopInfo();
                    WarningSopInfo wn = new WarningSopInfo();
                    boolean isWarnning = false;
                    HashMap map99ValueListnMap = JSONObject.parseObject(map99ValueListn, HashMap.class);
                    String map99_50 = "";
                    String map99_55 = "";
                    String map99_90 = "";
                    String map99_96 = "";
                    String map99_61 = "";
                    if (map99ValueListnMap.get("00081150") != null) {
                        map99_50 = map99ValueListnMap.get("00081150").toString();
                    }
                    if (map99ValueListnMap.get("00081155") != null) {
                        map99_55 = map99ValueListnMap.get("00081155").toString();
                    }
                    if (map99ValueListnMap.get("00081190") != null) {
                        map99_90 = map99ValueListnMap.get("00081190").toString();
                    }
                    if (map99ValueListnMap.get("00081196") != null) {
                        map99_96 = map99ValueListnMap.get("00081196").toString();
                    }
                    if (map99ValueListnMap.get("04000561") != null) {
                        map99_61 = map99ValueListnMap.get("04000561").toString();
                    }

                    if (StringUtils.isNotBlank(map99_96) && map99_96.startsWith("{") && map99_96.endsWith("}")) {
                        HashMap map99_96Map = JSONObject.parseObject(map99_96, HashMap.class);
                        isWarnning = true;
                        String map99_96MapValue = "";
                        if (map99_96Map.get("Value") != null) {
                            map99_96MapValue = map99_96Map.get("Value").toString();
                        }
                        wn.setWarningReason(map99_96MapValue);
                    }
                    if (StringUtils.isNotBlank(map99_61) && map99_61.startsWith("{") && map99_61.endsWith("}")) {
                        HashMap map99_61Map = JSONObject.parseObject(map99_61, HashMap.class);
                        isWarnning = true;
                        String map99_61MapValue = "";
                        if (map99_61Map.get("Value") != null) {
                            map99_61MapValue = map99_61Map.get("Value").toString();
                        }
                        if (isWarnning) {
                            wn.setOriginalAttributesSequence(map99_61MapValue);
                        } else {
                            sn.setOriginalAttributesSequence(map99_61MapValue);
                        }
                    }
                    if (StringUtils.isNotBlank(map99_50) && map99_50.startsWith("{") && map99_50.endsWith("}")) {
                        HashMap map99_50Map = JSONObject.parseObject(map99_50, HashMap.class);
                        String map99_50MapValue = "";
                        if (map99_50Map.get("Value") != null) {
                            map99_50MapValue = map99_50Map.get("Value").toString();
                        }
                        if (StringUtils.isNotBlank(map99_50MapValue) && map99_50MapValue.startsWith("[")
                                && map99_50MapValue.endsWith("]")) {
                            ArrayList map99_50MapValueList = JSONObject.parseObject(map99_50MapValue, ArrayList.class);
                            if (isWarnning) {
                                wn.setReferencedSOPClassUID(map99_50MapValueList);
                            } else {
                                sn.setReferencedSOPClassUID(map99_50MapValueList);
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(map99_55) && map99_55.startsWith("{") && map99_55.endsWith("}")) {
                        HashMap map99_55Map = JSONObject.parseObject(map99_55, HashMap.class);
                        String map99_55MapValue = "";
                        if (map99_55Map.get("Value") != null) {
                            map99_55MapValue = map99_55Map.get("Value").toString();
                        }
                        if (StringUtils.isNotBlank(map99_55MapValue) && map99_55MapValue.startsWith("[")
                                && map99_55MapValue.endsWith("]")) {
                            ArrayList map99_55MapValueList = JSONObject.parseObject(map99_55MapValue, ArrayList.class);
                            if (isWarnning) {
                                wn.setReferencedSOPInstanceUID(map99_55MapValueList);
                            } else {
                                sn.setReferencedSOPInstanceUID(map99_55MapValueList);
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(map99_90) && map99_90.startsWith("{") && map99_90.endsWith("}")) {
                        HashMap map99_90Map = JSONObject.parseObject(map99_90, HashMap.class);
                        String map99_90MapValue = "";
                        if (map99_90Map.get("Value") != null) {
                            map99_90MapValue = map99_90Map.get("Value").toString();
                        }
                        if (StringUtils.isNotBlank(map99_90MapValue) && map99_90MapValue.startsWith("[")
                                && map99_90MapValue.endsWith("]")) {
                            ArrayList map99_90MapValueList = JSONObject.parseObject(map99_90MapValue, ArrayList.class);
                            if (isWarnning) {
                                wn.setRetrieveURL(map99_90MapValueList);
                            } else {
                                sn.setRetrieveURL(map99_90MapValueList);
                            }
                        }
                    }
                    if (isWarnning) {
                        warningSopInfoList.add(wn);
                    } else {
                        successSopInfoList.add(sn);
                    }
                }
            }

        }
    }

    /**
     * 202情况下返回组装好的messageMap
     */
    public HashMap<String, Object> getMessageMap(FileUploadMessage uploadMessage) {

        HashMap<String, Object> mapApi = new HashMap<String, Object>();
        HashMap<String, Object> mapMq = new HashMap<String, Object>();
        HashMap<String, Object> mapMsg = new HashMap<String, Object>();

        List<String> retrieveURL = uploadMessage.getSuccessSopInfoList().get(0).getRetrieveURL();
        String successUrl = "";
        if (retrieveURL != null && retrieveURL.size() > 0) {
            successUrl = StringUtils.isNotBlank(retrieveURL.get(0)) ? retrieveURL.get(0) : "";
        }
        String studyUID = WordUtils.getSubUtilSimple(successUrl, "studies/(.*?)/series");
        mapMsg.put("studyUID", studyUID);
        String seriesUID = WordUtils.getSubUtilSimple(successUrl, "series/(.*?)/instances");
        mapMsg.put("seriesUID", seriesUID);
        String sopUID = WordUtils.getSubUtilSimple(successUrl, "instances/(.*?)$");
        mapMsg.put("sopUID", sopUID);
        mapMsg.put("userId",FLAGS.getClientUID());
        mapMsg.put("dcm4cheeUrl",FLAGS.getArchiveUrl());

        mapMq.put("seriesUID", seriesUID);
        mapMq.put("code", ClientInfoEnum.FILE_ARC_SUCCESS.getCode());
        mapMq.put("message", JSONObject.toJSONString(mapMsg));
        mapMq.put("level","info");

        mapApi.put("userId", FLAGS.getClientUID());
        mapApi.put("message",JSONObject.toJSONString(mapMq));
        return mapApi;
    }






}
