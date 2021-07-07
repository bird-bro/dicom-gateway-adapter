package org.bird.gateway;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.entity.ResponseMessage;
import org.bird.gateway.flags.Configurator;
import org.bird.gateway.flags.Flags;
import org.bird.gateway.retry.RetryService;
import org.bird.adapter.utils.EhcacheUtils;
import org.bird.gateway.utils.GatewayApiUtils;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author bird
 * @date 2021-7-2 13:23
 **/
@Slf4j
public class GatewayApi {

    private static final GatewayApiUtils API_UTILS = new GatewayApiUtils();
    private static final Flags FLAGS = Configurator.Configurator();
    private static final EhcacheUtils CACHE_TOKEN = new EhcacheUtils("cache.token");
    private static final EhcacheUtils CACHE_STOW_RS = new EhcacheUtils("cache.stowrs");
    private static final SimpleDateFormat SIMPLE_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

    public static String getToken() {
        try {
            String token = CACHE_TOKEN.GetString("token");
            if (token.isBlank()) {
                String url = FLAGS.getGatewayApiOauth() + "/sso/getToken";
                JSONObject response = API_UTILS.getToken(url, FLAGS.getClientAk(), FLAGS.getClientSk());
                if(200 == response.getInteger("code")){
                    token = response.getString("data");
                    CACHE_TOKEN.Put("token", token);
                } else {
                    throw new Exception(response.getString("msg"));
                }
            }
            return token;
        }catch (Exception e){
            log.error(e.getMessage());
            return null;
        }
    }

    private static String getToken(boolean flag) {
        if (flag) {
            CACHE_TOKEN.removeAll();
        }
        return getToken();
    }



    public static void postHeartbeat() {
       String url = FLAGS.getGatewayApi() + "/actuator/health";
        try {
            String token = getToken();
            JSONObject response = API_UTILS.doGet(url, token, FLAGS.getClientAk(), FLAGS.getClientSk(), FLAGS.getClientEnv());
            if(200 != response.getInteger("statusCode")){
                getToken(true);
            }
            log.info("PostHeartbeat : " + response.toJSONString());
            RetryService.Start();
        }catch (UnknownHostException e){
            log.error("Network error：{}", e.getMessage());
        }catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    public static void postStowRSMessage(ResponseMessage responseMessage) {
        try {
            JSONObject jsonObject = new JSONObject(responseMessage.getUploadMessage());
            String seriesUid = jsonObject.getJSONObject("message").getString("seriesUID");
            if (!seriesUid.isBlank() && CACHE_STOW_RS.Exists(seriesUid)) {
                return;
            }
            String url = FLAGS.getGatewayApi() + "/api/sendMessage";
            log.info("--Stow Message JSON--" + jsonObject.toJSONString());
            String token = getToken();
            JSONObject response = API_UTILS.doPost(url, token, jsonObject.toJSONString(), FLAGS.getClientAk(), FLAGS.getClientSk(), FLAGS.getClientEnv());
            log.info("--PostStowRSMessage-- " + response.toJSONString());

            if (200 == response.getInteger("statusCode")) {
                CACHE_STOW_RS.Put(seriesUid, SIMPLE_DATE.format(new Date()));
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }




}
