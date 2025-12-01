package com.chenyg.middleware.sdk.infrastructure.git;

import com.alibaba.fastjson2.JSON;
import com.chenyg.middleware.sdk.infrastructure.git.dto.CommitCommentRequestDTO;
import com.chenyg.middleware.sdk.infrastructure.git.dto.SingleCommitResponseDTO;
import com.chenyg.middleware.sdk.types.utils.DefaultHttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-29  15:15
 */
public class GitRestAPIOperation implements BaseGitOperation{

    // 定义 Token 限制相关的常量
    private static final int MAX_DIFF_TOTAL_LENGTH = 80000; // 总 Diff 最大长度

    private static final int MAX_FILE_DIFF_LENGTH = 20000;  // 单个文件 Diff 最大长度

    private final Logger logger = LoggerFactory.getLogger(GitRestAPIOperation.class);

    private final String githubRepoUrl;

    private final String githubToken;

    private final String pullNumber;

    private SingleCommitResponseDTO cachedCommitResponse;


    public GitRestAPIOperation(String githubRepoUrl, String githubToken, String pullNumber) {
        this.githubRepoUrl = githubRepoUrl;
        this.githubToken = githubToken;
        this.pullNumber = pullNumber;
    }

    @Override
    public String diff() throws Exception {
        // 如果缓存为空，则发起请求
        if (cachedCommitResponse == null) {
            logger.info("Start to get diff from github api: {}", githubRepoUrl);
            String result = DefaultHttpUtil.executeGetRequest(this.githubRepoUrl, getHeaders());
            logger.info("Get diff from github api result: {}", result);
            cachedCommitResponse = JSON.parseObject(result, SingleCommitResponseDTO.class);
        }

        SingleCommitResponseDTO.CommitFile[] files = cachedCommitResponse.getFiles();
        if (files == null) {
            logger.warn("Get diff from github api result files is null");
            return "";
        }
        StringBuilder diffCode = new StringBuilder();
        int currentTotalLength = 0;

        for (SingleCommitResponseDTO.CommitFile file : files) {
            logger.info("Processing file: {}", file.getFilename());
            String patch = file.getPatch();

            // 1. 跳过 Patch 为空的特殊文件（如二进制、大文件）
            if (patch == null) {
                logger.warn("Skipping file {} due to null patch (binary or too large).", file.getFilename());
                continue;
            }

            // 2. 单个文件过长截断
            if (patch.length() > MAX_FILE_DIFF_LENGTH) {
                patch = patch.substring(0, MAX_FILE_DIFF_LENGTH) + "\n... (File truncated due to length limit)";
            }

            // 3. 总长度检查
            if (currentTotalLength + patch.length() > MAX_DIFF_TOTAL_LENGTH) {
                diffCode.append("待评审文件名称：").append(file.getFilename()).append("\n");
                diffCode.append("该文件变更代码：... (Skipped due to total token limit)\n");
                logger.warn("Total diff length limit reached. Stopping file processing.");
                break;
            }

            diffCode.append("待评审文件名称：").append(file.getFilename()).append("\n");
            diffCode.append("该文件变更代码：").append(patch).append("\n");
            
            currentTotalLength += patch.length();
        }
        logger.info("Diff process finished. Total length: {}", currentTotalLength);
        return diffCode.toString();

    }

    @Override
    public String writeResult(String result) throws Exception {
        // 如果是 PR，使用 PR Review API 发表总评
        if (pullNumber != null && !pullNumber.isEmpty()) {
            String prReviewUrl = getBaseRepoUrl() + "/pulls/" + pullNumber + "/reviews";
            Map<String, Object> reviewBody = new HashMap<>();
            reviewBody.put("body", result);
            reviewBody.put("event", "COMMENT"); // 或者 APPROVE / REQUEST_CHANGES

            logger.info("Writing PR Review to: {}", prReviewUrl);
            String response = DefaultHttpUtil.executePostRequest(prReviewUrl, getHeaders(), reviewBody);
            logger.info("PR Review response: {}", response);
            
            // PR Review 创建后，HTML URL 通常在返回的 JSON 中
            // 这里简单返回一个拼接的 URL
            return getBaseRepoUrl().replace("api.github.com/repos", "github.com") + "/pull/" + pullNumber;
        }

        // 否则回退到 Commit Comment 逻辑
        // 针对 Push 事件，发送 General Commit Comment（对整个提交的评论，不指定 path/position）
        CommitCommentRequestDTO request = new CommitCommentRequestDTO();
        request.setBody(result);
        
        // 构建评论 URL: /repos/{owner}/{repo}/commits/{sha}/comments
        // 对于 Push 事件，githubRepoUrl 已经是 /commits/{sha} 结尾
        String commentUrl = this.githubRepoUrl + "/comments";
        
        logger.info("写入 Push General Commit Comment: {}", commentUrl);
        String response = writeCommentRequest(request, commentUrl);
        logger.info("Push Comment response: {}", response);

        return commentUrl;
    }
    
    private String getBaseRepoUrl() {
        // 从 githubRepoUrl 截取基础 API URL: https://api.github.com/repos/{owner}/{repo}
        String url = this.githubRepoUrl;
        if (url.contains("/compare/")) {
            return url.substring(0, url.indexOf("/compare/"));
        }
        if (url.contains("/commits/")) {
            return url.substring(0, url.indexOf("/commits/"));
        }
        return url;
    }

    private String buildCommentUrl(SingleCommitResponseDTO.CommitFile file) {
        String baseUrl = this.githubRepoUrl;
        String sha = null;

        // 提取 Base URL 和 SHA
        if (baseUrl.contains("/commits/")) {
            // 格式: .../commits/{sha}
            int commitsIndex = baseUrl.indexOf("/commits/");
            baseUrl = baseUrl.substring(0, commitsIndex);
            sha = this.githubRepoUrl.substring(commitsIndex + 9);
        } else if (baseUrl.contains("/compare/")) {
            // 格式: .../compare/{base}...{head}
            // 从 raw_url 提取 SHA: https://github.com/owner/repo/raw/{sha}/path
            int compareIndex = baseUrl.indexOf("/compare/");
            baseUrl = baseUrl.substring(0, compareIndex);
            String rawUrl = file.getRaw_url();
            if (rawUrl != null) {
                int rawIndex = rawUrl.indexOf("/raw/");
                if (rawIndex > -1) {
                    String sub = rawUrl.substring(rawIndex + 5);
                    int slashIndex = sub.indexOf("/");
                    if (slashIndex > -1) {
                        sha = sub.substring(0, slashIndex);
                    }
                }
            }
        }

        if (sha == null) {
            // 如果无法提取 SHA，回退到简单的 URL 拼接，虽然可能会失败
            return this.githubRepoUrl + "/comments";
        }

        return baseUrl + "/commits/" + sha + "/comments";
    }

    private String writeCommentRequest(CommitCommentRequestDTO request, String commentUrl) throws Exception {
        String requestText = DefaultHttpUtil.executePostRequest(commentUrl, getHeaders(), request);
        return requestText;
    }

    /**
     * 获取commit信息
     *
     * @return 响应对象
     * @throws Exception
     */
    private SingleCommitResponseDTO getCommitResponse() throws Exception {
        if (cachedCommitResponse == null) {
            String result = DefaultHttpUtil.executeGetRequest(this.githubRepoUrl, getHeaders());
            cachedCommitResponse = JSON.parseObject(result, SingleCommitResponseDTO.class);
        }
        return cachedCommitResponse;
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.github+json ");
        headers.put("Authorization", "Bearer " + githubToken);
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }
}
