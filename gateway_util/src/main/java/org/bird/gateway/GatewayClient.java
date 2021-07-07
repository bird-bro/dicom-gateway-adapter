package org.bird.gateway;

import com.alibaba.fastjson.JSONObject;
import com.google.api.client.http.*;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.entity.ResponseMessage;
import org.bird.gateway.flags.Configurator;
import org.bird.gateway.flags.Flags;
import org.bird.gateway.retry.DelayStore;
import org.bird.gateway.utils.GatewayUtils;
import org.bird.gateway.utils.StowResponseUtils;
import org.bird.gateway.utils.StringUtils;
import org.dcm4che3.net.Status;
import org.json.JSONArray;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.bird.gateway.utils.ApiUtils.genReqDate;

/**
 * @author bird
 * @date 2021-7-2 11:41
 **/
@Slf4j
public class GatewayClient implements IGatewayClient{

    private static final Flags FLAGS = Configurator.configurator();


    // Factory to create HTTP requests with proper credentials.
    protected final HttpRequestFactory requestFactory;

    // Service prefix all dicomWeb paths will be appended to.
    private String serviceUrlPrefix;

    // The path for a StowRS request to be appened to serviceUrlPrefix.
    private final String stowPath;


    public GatewayClient(
            HttpRequestFactory requestFactory,
            @Annotations.GatewayAddr String serviceUrlPrefix,
            String stowPath) {
        this.requestFactory = requestFactory;
        this.serviceUrlPrefix = serviceUrlPrefix;
        this.stowPath = stowPath;
    }

    /**
     * Makes a WADO-RS call and returns the response InputStream.
     */
    @Override
    public InputStream wadoRs(String path) throws IGatewayClient.DicomGatewayException {
        try {
            HttpRequest httpRequest = requestFactory.buildGetRequest(new GenericUrl(serviceUrlPrefix + "/" + StringUtils.trim(path)));
            httpRequest.getHeaders().put("Accept", "application/dicom; transfer-syntax=*");
            HttpResponse httpResponse = httpRequest.execute();
            return httpResponse.getContent();
        } catch (HttpResponseException e) {
            throw new IGatewayClient.DicomGatewayException(
                    String.format("WADO-RS: %d, %s", e.getStatusCode(), e.getStatusMessage()),
                    e, e.getStatusCode(), Status.ProcessingFailure
            );
        } catch (IOException | IllegalArgumentException e) {
            throw new IGatewayClient.DicomGatewayException(e);
        }
    }

    /**
     * Makes a QIDO-RS call and returns a JSON array.
     */
    @Override
    public JSONArray qidoRs(String path) throws IGatewayClient.DicomGatewayException {
        try {
            HttpRequest httpRequest = requestFactory.buildGetRequest(new GenericUrl(serviceUrlPrefix + "/" + StringUtils.trim(path)));
            HttpResponse httpResponse = httpRequest.execute();
            // archive server can return 204 responses.
            if (httpResponse.getStatusCode() == HttpStatusCodes.STATUS_CODE_NO_CONTENT) {
                return new JSONArray();
            }
            return new JSONArray(CharStreams.toString(new InputStreamReader(httpResponse.getContent(), StandardCharsets.UTF_8)));
        } catch (HttpResponseException e) {
            throw new IGatewayClient.DicomGatewayException(String.format("QIDO-RS: %d, %s", e.getStatusCode(), e.getStatusMessage()),
                    e, e.getStatusCode(), Status.UnableToCalculateNumberOfMatches);
        } catch (IOException | IllegalArgumentException e) {
            throw new IGatewayClient.DicomGatewayException(e);
        }
    }

    /**
     * Makes a STOW-RS call.
     * DICOM "Type" parameter:
     * http://dicom.nema.org/medical/dicom/current/output/html/part18.html#sect_6.6.1.1.1
     *
     * @param in The DICOM input stream.
     */
    @Override
    public void stowRs(InputStream in) throws IOException, IGatewayClient.DicomGatewayException {

        DelayStore delayStore = new DelayStore(in);
        InputStream inputStream = delayStore.sourceStream;

        GenericUrl url = new GenericUrl(StringUtils.joinPath(serviceUrlPrefix, this.stowPath));

        MultipartContent content = new MultipartContent();
        content.setMediaType(new HttpMediaType("multipart/related; type=\"application/dicom\""));
        content.setBoundary(UUID.randomUUID().toString());
        InputStreamContent dicomStream = new InputStreamContent("application/dicom", inputStream);
        content.addPart(new MultipartContent.Part(dicomStream));
        HttpResponse resp = null;

        try {
            HttpRequest httpRequest = requestFactory.buildPostRequest(url, content);
            httpRequest.setConnectTimeout(15000);
            log.info("--STOW-RS HEAD--:  " + httpRequest.getHeaders().toString());
            resp = httpRequest.execute();
            //Archive response
            if (resp != null) {
                log.info("--STOW_RS response-- code:{}; message:{}", resp.getStatusCode(), resp.getStatusMessage());
                if(500 == resp.getStatusCode()){
                    delayStore.toDelayRetry();
                    return;
                }
                //解析,MQ
//                ResponseMessage message = new StowResponseUtils(resp.getStatusCode(), resp.getContent()).responseMessage();
//                GatewayUtils.postStowRSMessage(message);
            }
        } catch (HttpResponseException e) {
            log.error(String.format("Service error: %s", e.getMessage()));
            delayStore.toDelayRetry();
            throw new DicomGatewayException(String.format("StowRs: %d, %s", e.getStatusCode(), e.getStatusMessage()),
                    e, e.getStatusCode(), Status.ProcessingFailure);
        }catch (UnknownHostException e) {
            log.error(String.format("Network error: %s", e.getMessage()));
            delayStore.toDelayRetry();
        }catch (IOException e) {
            delayStore.toDelayRetry();
            throw new IGatewayClient.DicomGatewayException(e);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if((resp) != null){
                    resp.disconnect();
                }
                delayStore.close();
            }catch (IOException e) {
                throw new IGatewayClient.DicomGatewayException(e);
            }
        }
    }



}
