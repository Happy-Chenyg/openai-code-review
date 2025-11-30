package com.chenyg.middleware.sdk.types.utils;

import com.alibaba.fastjson2.JSON;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
     * @param uri     请求的URI
     * @param headers 请求头
     * @return 响应内容, 字符串类型
     * @throws Exception
     */
    public static String executeGetRequest(String uri, Map<String, String> headers) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        //设置请求头
        headers.forEach((key, value) -> connection.setRequestProperty(key, value));

        connection.setDoOutput(true);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();

        return content.toString();
    }

    /**
     * 执行POST请求
     *
     * @param uri     请求的URI
     * @param headers 请求头
     * @param body    请求体
     * @return 响应内容, 字符串类型
     * @throws Exception
     */
    public static String executePostRequest(String uri, Map<String, String> headers, Object body) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        // 设置默认请求头
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");

        // 设置自定义请求头
        if (headers != null) {
            headers.forEach(connection::setRequestProperty);
        }

        connection.setDoOutput(true);

        // 写入请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(body).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 处理响应
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } else {
            // 读取错误流
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new RuntimeException("HTTP Request Failed with code " + responseCode + ": " + response.toString());
            }
        }
    }
}
