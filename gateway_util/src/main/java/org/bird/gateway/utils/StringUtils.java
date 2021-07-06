package org.bird.gateway.utils;

import com.google.common.base.CharMatcher;

/**
 * @author bird
 * @date 2021-7-2 9:54
 **/
public class StringUtils {

    public static String trim(String value) {
        return CharMatcher.is('/').trimFrom(value);
    }

    public static String joinPath(String serviceUrlPrefix, String path){
        return StringUtils.trim(serviceUrlPrefix) + "/" + StringUtils.trim(path);
    }

}
