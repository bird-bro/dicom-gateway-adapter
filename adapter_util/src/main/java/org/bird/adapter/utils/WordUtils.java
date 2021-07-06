package org.bird.adapter.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bird
 * @date 2021-7-2 16:23
 **/
public class WordUtils {

    /**
     * 判断一个字符是否是中文,根据字节码判断
     */
    public static boolean isChinese(char c) {
        return c >= 0x4E00 &&  c <= 0x9FA5;
    }

    /**
     * 判断一个字符串是否含有中文
     */
    public static boolean isChinese(String str) {
        if (str == null) {return false;}
        for (char c : str.toCharArray()) {
            // 有一个中文字符就返回
            if (isChinese(c)) {return true;}
        }
        return false;
    }
    /**
     * 正则匹配获取数据
     */
    public static String getSubUtilSimple(String soap,String rgex){
        // 匹配的模式
        Pattern pattern = Pattern.compile(rgex);
        Matcher m = pattern.matcher(soap);
        while(m.find()){
            return m.group(1);
        }
        return "";
    }

}
