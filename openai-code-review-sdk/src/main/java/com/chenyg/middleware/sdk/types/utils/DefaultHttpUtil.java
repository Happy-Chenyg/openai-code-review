package com.chenyg.middleware.sdk.types.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-29  15:24
 */
public class DefaultHttpUtil {

    /**
     * 执行GET请求
     *
     * @param uri 请求的URI
     * @param headers 请求头
     * @return 响应内容,字符串类型
     * @throws Exception
     */
    public static String executeGetRequest(String uri, Map<String,String> headers) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        //设置请求头
        headers.forEach((key, value) -> connection.setRequestProperty(key, value));

        connection.setDoOutput(true);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();

        return content.toString();
    }
}
