package org.bird.gateway.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bird.gateway.common.EncryptUtils;
import org.bird.gateway.common.entity.RequestTokenInfo;
import org.eclipse.jetty.util.StringUtil;
/**
 * @author bird
 * @date 2021-7-1 15:58
 **/
public class GatewayApiUtils {

    public JSONObject getToken(String httpUrl, String accessKey, String secretKey) throws Exception {
        String reqJson = JSONObject.toJSONString(genReqDate(accessKey, secretKey));
        HttpPost httpPost = new HttpPost(httpUrl);
        //处理字符编码
        StringEntity entity = new StringEntity(reqJson, "utf-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpResponse resp = client.execute(httpPost);
        HttpEntity he = resp.getEntity();
        return JSONObject.parseObject(EntityUtils.toString(he, "UTF-8"));
    }

    /**
     * @param httpUrl   请求地址
     * @param token     请求token
     * @param accessKey ak
     * @param secretKey sk
     * @param env       环境，test or prod
     * @return
     * @throws Exception
     */
    public JSONObject doGet(String httpUrl, String token, String accessKey, String secretKey, String env) throws Exception {
        return doHttpResq(1, httpUrl, null, token, accessKey, secretKey, env);
    }

    /**
     * @param httpUrl   请求地址
     * @param token     请求token
     * @param reqJson   请求json
     * @param accessKey ak
     * @param secretKey sk
     * @param env       环境，test or prod
     * @return
     * @throws Exception
     */
    public JSONObject doPost(String httpUrl, String token, String reqJson, String accessKey, String secretKey, String env) throws Exception {
        return doHttpResq(0, httpUrl, reqJson, token, accessKey, secretKey, env);
    }

    /**
     * @param isGetOrPost
     * @param httpUrl
     * @param reqJson
     * @param token
     * @param accessKey
     * @param secretKey
     * @param env
     * @return
     * @throws Exception
     */
    private JSONObject doHttpResq(int isGetOrPost, String httpUrl, String reqJson, String token, String accessKey, String secretKey, String env) throws Exception {

        HttpRequestBase httpRequestBase = null;
        JSONObject headerJson = genReqDate(accessKey, secretKey);
        if (isGetOrPost == 0) {
            httpRequestBase = new HttpPost(httpUrl);
            //解决中文乱码问题
            StringEntity entity = new StringEntity(reqJson, "utf-8");
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            ((HttpPost) httpRequestBase).setEntity(entity);
        } else if (isGetOrPost == 1) {
            httpRequestBase = new HttpGet(httpUrl);
        }
        if (StringUtil.isNotBlank(token)) {
            httpRequestBase.setHeader("Authorization-" + env, token);
            httpRequestBase.setHeader("accessKey", headerJson.getString("accessKey"));
            httpRequestBase.setHeader("timestamp", headerJson.getString("timestamp"));
            httpRequestBase.setHeader("sign", headerJson.getString("sign"));
        }
        CloseableHttpClient client = HttpClients.createDefault();
        HttpResponse resp = client.execute(httpRequestBase);
        HttpEntity he = resp.getEntity();
        JSONObject result = JSONObject.parseObject(EntityUtils.toString(he, "UTF-8"));
        result.put("statusCode", resp.getStatusLine().getStatusCode());
        return result;
    }



    public static JSONObject genReqDate(String accessKey, String secretKey) {
        String salt = "rimag-api";
        RequestTokenInfo requestTokenInfo = new RequestTokenInfo();
        requestTokenInfo.setAccessKey(accessKey);
        // 当前时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        requestTokenInfo.setTimestamp(timestamp);

        // 生成签名
        String sign = EncryptUtils.getCiphertext("SHA-256", secretKey + salt + timestamp);
        requestTokenInfo.setSign(sign);

        return JSONObject.parseObject(JSONObject.toJSONString(requestTokenInfo));
    }


}
