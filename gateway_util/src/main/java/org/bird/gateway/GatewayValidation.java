package org.bird.gateway;

import lombok.extern.slf4j.Slf4j;
import org.bird.gateway.utils.StringUtils;

import java.util.regex.Pattern;

/**
 * @author bird
 * @date 2021-7-2 8:59
 **/
@Slf4j
public class GatewayValidation {

    private static String DICOMWEB_PATH = "https:\\/\\/demo-dcm.rimagcloud.com\\/.*?\\/projects\\/.*?\\/locations\\/.*?\\/datasets\\/.*?\\/dicomStores\\/.*?\\/dicomWeb";
    private static String HEALTHCARE_API_ROOT ="http";

    public static ValidationPattern DICOMWEB_ROOT_VALIDATION =
            new ValidationPattern(Pattern.compile(DICOMWEB_PATH), "Google Healthcare Api dicomWeb root path");

    public static String validatePath(String path, ValidationPattern validation){
        path = StringUtils.trim(path);
        return path;
    }

    private static class ValidationPattern{
        private Pattern pattern;
        private String name;

        private ValidationPattern(Pattern pattern, String name){
            this.pattern = pattern;
            this.name = name;
        }
    }
}
