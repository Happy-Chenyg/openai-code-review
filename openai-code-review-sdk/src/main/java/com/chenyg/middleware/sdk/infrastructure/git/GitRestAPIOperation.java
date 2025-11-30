package com.chenyg.middleware.sdk.infrastructure.git;

import com.alibaba.fastjson2.JSON;
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
}
