package org.bird.gateway;

import com.google.api.client.http.*;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.flags.Configurator;
import org.bird.gateway.flags.Flags;
import org.bird.gateway.retry.DelayStore;
import org.bird.gateway.utils.StringUtils;
import org.dcm4che3.net.Status;
import org.json.JSONArray;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    private final String mppsUrl;


    public GatewayClient(
            HttpRequestFactory requestFactory,
            String mppsUrl,
            @Annotations.GatewayAddr String serviceUrlPrefix,
            String stowPath) {
        this.requestFactory = requestFactory;
        this.mppsUrl = mppsUrl;
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
    public void stowRs(InputStream in) throws IOException, IGatewayClient.DicomGatewayException{

        DelayStore delayStore = new DelayStore(in);
        InputStream inputStream = delayStore.sourceStream;

        //GenericUrl url = new GenericUrl(StringUtils.joinPath(serviceUrlPrefix, this.stowPath));
        GenericUrl url = new GenericUrl("http://172.16.55.20:8080/dcm4chee-arc/aets/DCM4CHEE/rs/studies");

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

    /**
     * Send MPPS to server
     * DICOM "Type" parameter:
     * http://dicom.nema.org/medical/dicom/current/output/html/part18.html#sect_6.6.1.1.1
     *
     * @param in The DICOM input stream.
     */
    @Override
    public void sendMpps(InputStream in, String cmd) throws IOException, IGatewayClient.DicomGatewayException {
        URL url = new URL(this.mppsUrl);
        HttpURLConnection conn = null;
        final String BOUNDARY = UUID.randomUUID().toString();

        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            // 设置请求头参数
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            OutputStream out = new DataOutputStream(conn.getOutputStream());

            StringBuffer reqBuffer = new StringBuffer();
            reqBuffer.append("\r\n--").append(BOUNDARY).append("\r\n");
            reqBuffer.append("Content-Disposition: form-data;name=\"mpps\"\r\n\r\n" + cmd);
            reqBuffer.append("\r\n--").append(BOUNDARY).append("\r\n");
            reqBuffer.append("Content-Disposition: form-data;name=\"file\";filename=\"" + BOUNDARY + "\"\r\n");
            reqBuffer.append("Content-Type:application/octet-stream\r\n\r\n");
            out.write(reqBuffer.toString().getBytes(StandardCharsets.UTF_8));

            DataInputStream din = new DataInputStream(in);
            int bytes = 0;
            byte[] outBuf = new byte[1024];
            while ((bytes = din.read(outBuf)) != -1) {
                out.write(outBuf, 0, bytes);
            }
            din.close();

            out.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();

            // response
            StringBuffer respBuffer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                respBuffer.append(line).append("\n");
            }
            String resp = respBuffer.toString();
            reader.close();
            reader = null;

            log.info(resp);

        } catch (HttpResponseException e) {
            log.error(String.format("Service error: %s", e.getMessage()));
            throw new DicomGatewayException(String.format("StowRs: %d, %s", e.getStatusCode(), e.getStatusMessage()),
                    e, e.getStatusCode(), Status.ProcessingFailure);
        }catch (UnknownHostException e) {
            log.error(String.format("Network error: %s", e.getMessage()));
        }catch (IOException e) {
            throw new IGatewayClient.DicomGatewayException(e);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(conn != null){
                conn.disconnect();
                conn = null;
            }
        }

//        try {
//
//            String fileName = "D:\\workspace\\java\\dcm4che-5.24.0\\bin\\test.dcm";
//            File file = new File(fileName);
//
//            // 换行符
//            final String newLine = "\r\n";
//            final String boundaryPrefix = "--";
//            // 定义数据分隔线
//            String BOUNDARY = "========7d4a6d158c9";
//            // 服务器的域名
//            URL url = new URL(this.mppsUrl);
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            // 设置为POST情
//            conn.setRequestMethod("POST");
//            // 发送POST请求必须设置如下两行
//            conn.setDoOutput(true);
//            conn.setDoInput(true);
//            conn.setUseCaches(false);
//            // 设置请求头参数
//            conn.setRequestProperty("connection", "Keep-Alive");
//            conn.setRequestProperty("Charsert", "UTF-8");
//            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
//
//            OutputStream out = new DataOutputStream(conn.getOutputStream());
//
//            // 上传文件
//            StringBuilder sb = new StringBuilder();
//            sb.append(boundaryPrefix);
//            sb.append(BOUNDARY);
//            sb.append(newLine);
//            // 文件参数,photo参数名可以随意修改
//            sb.append("Content-Disposition: form-data;name=\"file\";filename=\"" + fileName
//                    + "\"" + newLine);
//            sb.append("Content-Type:application/octet-stream");
//            // 参数头设置完以后需要两个换行，然后才是参数内容
//            sb.append(newLine);
//            sb.append(newLine);
//
//            // 将参数头的数据写入到输出流中
//            out.write(sb.toString().getBytes());
//
//            // 数据输入流,用于读取文件数据
//            DataInputStream din = new DataInputStream(new FileInputStream(
//                    file));
//            byte[] bufferOut = new byte[1024];
//            int bytes = 0;
//            // 每次读1KB数据,并且将文件数据写入到输出流中
//            while ((bytes = din.read(bufferOut)) != -1) {
//                out.write(bufferOut, 0, bytes);
//            }
//            // 最后添加换行
//            out.write(newLine.getBytes());
//            din.close();
//
//            // 定义最后数据分隔线，即--加上BOUNDARY再加上--。
//            byte[] end_data = (newLine + boundaryPrefix + BOUNDARY + boundaryPrefix + newLine)
//                    .getBytes();
//            // 写上结尾标识
//            out.write(end_data);
//            out.flush();
//            out.close();
//
//            // 定义BufferedReader输入流来读取URL的响应
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    conn.getInputStream()));
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//
//        } catch (Exception e) {
//            System.out.println("发送POST请求出现异常！" + e);
//            e.printStackTrace();
//        }
    }

}
