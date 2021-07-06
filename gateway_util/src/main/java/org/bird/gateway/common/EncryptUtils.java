package org.bird.gateway.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author bird
 * @date 2021-7-1 16:37
 **/
@Slf4j
public class EncryptUtils {

    public static String getCiphertext(String method, String str) {
        String encodestr = "";

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(method);
            messageDigest.update(str.getBytes("UTF-8"));
            encodestr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException var5) {
            log.error("", var5);
        } catch (UnsupportedEncodingException var6) {
            log.error("", var6);
        }

        return encodestr;
    }

    private static String byte2Hex(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();

        for(int i = 0; i < bytes.length; ++i) {
            String temp = Integer.toHexString(bytes[i] & 255);
            if (temp.length() == 1) {
                stringBuffer.append("0");
            }

            stringBuffer.append(temp);
        }

        return stringBuffer.toString();
    }

    public static String getSHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);

            String hashtext;
            for(hashtext = no.toString(16); hashtext.length() < 32; hashtext = "0" + hashtext) {
            }

            return hashtext;
        } catch (NoSuchAlgorithmException var5) {
            log.error("Exception thrown for incorrect algorithm:", var5);
            return null;
        }
    }

    public static String aesEncrypt(String plaintext, String secretKey, String encryptMode) {
        try {
            if (StringUtils.isEmpty(secretKey)) {
                return null;
            } else {
                secretKey = getMD5(secretKey);
                byte[] raw = secretKey.getBytes("utf-8");
                SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
                Cipher cipher = Cipher.getInstance("AES/" + encryptMode + "/PKCS5Padding");
                if ("ECB".equals(encryptMode)) {
                    cipher.init(1, skeySpec);
                } else {
                    IvParameterSpec iv = new IvParameterSpec(secretKey.getBytes("utf-8"));
                    cipher.init(1, skeySpec, iv);
                }

                byte[] encrypted = cipher.doFinal(plaintext.getBytes("utf-8"));
                return Base64.getEncoder().encodeToString(encrypted);
            }
        } catch (Exception var7) {
            log.error("AES加密异常", var7);
            return null;
        }
    }

    public static String aesDecrypt(String cipertext, String secretKey, String encryptMode) {
        try {
            if (StringUtils.isEmpty(secretKey)) {
                return null;
            } else {
                secretKey = getMD5(secretKey);
                byte[] raw = secretKey.getBytes("utf-8");
                SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
                Cipher cipher = Cipher.getInstance("AES/" + encryptMode + "/PKCS5Padding");
                if ("ECB".equals(encryptMode)) {
                    cipher.init(2, skeySpec);
                } else {
                    IvParameterSpec iv = new IvParameterSpec(secretKey.getBytes("utf-8"));
                    cipher.init(2, skeySpec, iv);
                }

                byte[] encrypted1 = Base64.getDecoder().decode(cipertext);

                try {
                    byte[] original = cipher.doFinal(encrypted1);
                    String originalString = new String(original, "utf-8");
                    return originalString;
                } catch (Exception var9) {
                    log.error("AES解密异常", var9);
                    return null;
                }
            }
        } catch (Exception var10) {
            log.error("AES解密异常", var10);
            return null;
        }
    }

    public static String getMD5(String s) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        try {
            byte[] btInput = s.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;

            for(int i = 0; i < j; ++i) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 15];
                str[k++] = hexDigits[byte0 & 15];
            }

            return (new String(str)).substring(8, 24);
        } catch (Exception var10) {
            log.error("MD5异常", var10);
            return null;
        }
    }

}
