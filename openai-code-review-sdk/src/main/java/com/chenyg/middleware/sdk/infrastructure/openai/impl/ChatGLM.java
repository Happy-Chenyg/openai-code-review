package com.chenyg.middleware.sdk.infrastructure.openai.impl;

import com.alibaba.fastjson2.JSON;
import com.chenyg.middleware.sdk.infrastructure.openai.IOpenAI;
import com.chenyg.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import com.chenyg.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;
import com.chenyg.middleware.sdk.types.utils.BearerTokenUtils;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatGLM implements IOpenAI {

    private final String apiHost;
    private final String apiKeySecret;

    public ChatGLM(String apiHost, String apiKeySecret) {
        this.apiHost = apiHost;
        this.apiKeySecret = apiKeySecret;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception {
        String token = BearerTokenUtils.getToken(apiKeySecret);
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(apiHost);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = JSON.toJSONString(requestDTO).getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 429) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        System.err.println("Rate limit exceeded (429). Retrying in 5 seconds... (Attempt " + retryCount + "/" + maxRetries + ")");
                        Thread.sleep(5000);
                        continue;
                    }
                }
                
                if (responseCode >= 200 && responseCode < 300) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    return JSON.parseObject(content.toString(), ChatCompletionSyncResponseDTO.class);
                } else {
                     // 读取错误信息
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String errorLine;
                    StringBuilder errorContent = new StringBuilder();
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorContent.append(errorLine);
                    }
                    errorReader.close();
                    throw new RuntimeException("Request failed with code: " + responseCode + ", message: " + errorContent.toString());
                }

            } catch (Exception e) {
                // 如果是 IO 异常且不是我们刚才手动处理的错误（比如连接超时等），也可以选择重试
                // 这里主要针对 429 处理，其他异常直接抛出
                throw e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        throw new RuntimeException("Failed to get completion after " + maxRetries + " retries");
    }

}
