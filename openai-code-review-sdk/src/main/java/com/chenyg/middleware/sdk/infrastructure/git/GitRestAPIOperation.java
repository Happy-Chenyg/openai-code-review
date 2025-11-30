package com.chenyg.middleware.sdk.infrastructure.git;

import com.alibaba.fastjson2.JSON;
import com.chenyg.middleware.sdk.infrastructure.git.dto.CommitCommentRequestDTO;
import com.chenyg.middleware.sdk.infrastructure.git.dto.SingleCommitResponseDTO;
import com.chenyg.middleware.sdk.types.utils.DefaultHttpUtil;
import com.chenyg.middleware.sdk.types.utils.DiffParseUtil;
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

    private final Logger logger = LoggerFactory.getLogger(GitRestAPIOperation.class);

    private final String githubRepoUrl;

    private final String githubToken;

    private SingleCommitResponseDTO cachedCommitResponse;

    public GitRestAPIOperation(String githubRepoUrl, String githubToken) {
        this.githubRepoUrl = githubRepoUrl;
        this.githubToken = githubToken;
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
        for (SingleCommitResponseDTO.CommitFile file : files) {
            logger.info("Processing file: {}", file.getFilename());
            diffCode.append("待评审文件名称：").append(file.getFilename()).append("\n");
            diffCode.append("该文件变更代码：").append(file.getPatch()).append("\n");
        }
        logger.info("Diff process finished.");
        return diffCode.toString();

    }

    @Override
    public String writeResult(String result) throws Exception {
        // 确保有数据（通常 diff() 会先被调用）
        SingleCommitResponseDTO responseDTO = getCommitResponse();
        SingleCommitResponseDTO.CommitFile[] files = responseDTO.getFiles();
        for (SingleCommitResponseDTO.CommitFile file : files) {
            String patch = file.getPatch();
            // Commit Comment API需要的是变更字符串中的索引
            int diffPositionIndex = DiffParseUtil.parseLastDiffPosition(patch);
            CommitCommentRequestDTO request = new CommitCommentRequestDTO();
            request.setBody(result);
            request.setPath(file.getFilename());
            request.setPosition(diffPositionIndex);
            logger.info("写入注释请求参数：{}", JSON.toJSONString(request));

            // 构建正确的评论 URL
            String commentUrl = buildCommentUrl(file);
            writeCommentRequest(request, commentUrl);
            logger.info("写入评审到注释区域处理完成，注释结果：{}", request);
            // 由于之前的评审是一次性评审多次，所以这里只处理一次，未来优化
            break;
        }

        return responseDTO.getHtml_url();
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
