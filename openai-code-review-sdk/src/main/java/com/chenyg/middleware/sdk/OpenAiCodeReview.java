package com.chenyg.middleware.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class OpenAiCodeReview {
        public static void main(String[] args) throws InterruptedException, IOException {
            System.out.println("测试执行");
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

        }
}
