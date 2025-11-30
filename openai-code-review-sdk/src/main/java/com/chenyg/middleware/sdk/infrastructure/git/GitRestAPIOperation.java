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

    public GitRestAPIOperation(String githubRepoUrl, String githubToken) {
        this.githubRepoUrl = githubRepoUrl;
        this.githubToken = githubToken;
    }
    @Override
    public String diff() throws Exception {
        logger.info("Start to get diff from github api: {}", githubRepoUrl);
        Map<String, String> params = new HashMap<String, String>();
        params.put("Accept", "application/vnd.github+json ");
        params.put("Authorization", "Bearer " + githubToken);
        params.put("X-GitHub-Api-Version", "2022-11-28");

        String result = DefaultHttpUtil.executeGetRequest(this.githubRepoUrl, params);
        logger.info("Get diff from github api result: {}", result);
        SingleCommitResponseDTO singleCommitResponseDTO = JSON.parseObject(result, SingleCommitResponseDTO.class);
        SingleCommitResponseDTO.CommitFile[] files = singleCommitResponseDTO.getFiles();
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
            logger.info("写入注释请求参数：{}",JSON.toJSONString( request));
            writeCommentRequest(request);
            logger.info("写入评审到注释区域处理完成，注释结果：{}",request);
            // 由于之前的评审是一次性评审多次，所以这里只处理一次，未来优化
            break;
        }

        return responseDTO.getHtml_url();
    }

    private String writeCommentRequest(CommitCommentRequestDTO request) throws Exception {

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/vnd.github+json ");
        headers.put("Authorization", "Bearer " + githubToken);
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        String requestText = DefaultHttpUtil.executePostRequest(this.githubRepoUrl + "/comments", headers, request);
        return requestText;
    }

    /**
     * 获取commit信息
     * @return 响应对象
     * @throws Exception
     */
    private SingleCommitResponseDTO getCommitResponse() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/vnd.github+json ");
        headers.put("Authorization", "Bearer " + githubToken);
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        String result = DefaultHttpUtil.executeGetRequest(this.githubRepoUrl, headers);
        return JSON.parseObject(result, SingleCommitResponseDTO.class);
    }
}
