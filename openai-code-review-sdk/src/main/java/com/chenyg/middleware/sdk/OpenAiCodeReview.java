package com.chenyg.middleware.sdk;

import com.alibaba.fastjson2.JSON;
import com.chenyg.middleware.sdk.domain.model.ChatCompletionRequest;
import com.chenyg.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import com.chenyg.middleware.sdk.domain.model.Model;
import com.chenyg.middleware.sdk.types.utils.BearerTokenUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


public class OpenAiCodeReview {
        public static void main(String[] args) throws Exception {
            System.out.println("openai 代码评审，测试执行");

            String token = System.getenv("GITHUB_TOKEN");
            if (null == token || token.isEmpty()) {
                throw new RuntimeException("token is null");
            }

            // 1. 代码检出 比较当前提交与上一次提交的代码差异 ProcessBuilder用于创建操作系统进程
            ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD");
            // 设置工作目录为当前目录（"."表示当前路径）
            processBuilder.directory(new File("."));

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            StringBuilder diffCode = new StringBuilder();
            // 逐行读取Git命令执行后的输出结果，将每一行代码差异内容追加到StringBuilder中
            while ((line = reader.readLine()) != null) {
                diffCode.append(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exited with code:" + exitCode);

            System.out.println("评审代码：" + diffCode.toString());
            // 2. chatglm 代码评审
            String log = codeReview(diffCode.toString());
            System.out.println("code review：" + log);

            // 3. 写入评审日志
            String logUrl = writeLog(token, log);
            System.out.println("writeLog：" + logUrl);

        }
    private static String codeReview(String diffCode) throws Exception {

        String apiKeySecret = "024ad122918641db996a1ab8ae9b12e8.ZITZaVYdYjfS2Zqz";
        String token = BearerTokenUtils.getToken(apiKeySecret);

        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
        connection.setDoOutput(true);

        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setModel(Model.GLM_4_FLASH.getCode());
        chatCompletionRequest.setMessages(new ArrayList<ChatCompletionRequest.Prompt>() {
            private static final long serialVersionUID = -7988151926241837899L;

            {
                add(new ChatCompletionRequest.Prompt("user", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。代码如下:"));
                add(new ChatCompletionRequest.Prompt("user", diffCode));
            }
        });

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(chatCompletionRequest).getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        int responseCode = connection.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;

        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        connection.disconnect();

        System.out.println("评审结果：" + content.toString());

        ChatCompletionSyncResponse response = JSON.parseObject(content.toString(), ChatCompletionSyncResponse.class);
        return response.getChoices().get(0).getMessage().getContent();
    }

    private static String writeLog(String token, String log) throws Exception {
        Git git = Git.cloneRepository()
                .setURI("https://github.com/Happy-Chenyg/openai-code-rewiew-log.git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();

        // 根据当前日期创建文件夹
        String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        File dateFolder = new File("repo/" + dateFolderName);
        if (!dateFolder.exists()) {
            dateFolder.mkdirs();
        }

        // 生成一个12位随机字符串作为文件名
        String fileName = generateRandomString(12) + ".md";
        // 在指定日期文件中创建一个新文件
        File newFile = new File(dateFolder, fileName);
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(log);
        }

        git.add().addFilepattern(dateFolderName + "/" + fileName).call();
        git.commit().setMessage("Add new file via GitHub Actions").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();

        System.out.println("Changes have been pushed to the repository.");

        return "https://github.com/Happy-Chenyg/openai-code-rewiew-log/blob/master/" + dateFolderName + "/" + fileName;
    }

    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }


}
