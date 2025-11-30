package com.chenyg.middleware.sdk;


import com.chenyg.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import com.chenyg.middleware.sdk.infrastructure.git.BaseGitOperation;
import com.chenyg.middleware.sdk.infrastructure.git.GitCommand;
import com.chenyg.middleware.sdk.infrastructure.git.GitRestAPIOperation;
import com.chenyg.middleware.sdk.infrastructure.openai.IOpenAI;
import com.chenyg.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import com.chenyg.middleware.sdk.infrastructure.weixin.WeiXin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class OpenAiCodeReview {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

    // 配置配置
    private String weixin_appid = "wx9ff0cb86a77c5b7d";
    private String weixin_secret = "789e758666ba3a027b56546e29ec056d";
    private String weixin_touser = "oXKusvgkV8zViivFvqr_iqgF2jx0";
    private String weixin_template_id = "aGwSzONomySlbHmeLarVRwhdglG2uRA8LtC7X_twn7E";

    // ChatGLM 配置
    private String chatglm_apiHost = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private String chatglm_apiKeySecret = "";

    // Github 配置
    private String github_review_log_uri;
    private String github_token;

    // 工程配置 - 自动获取
    private String github_project;
    private String github_branch;
    private String github_author;

    public static void main(String[] args) throws Exception {
        GitCommand gitCommand = new GitCommand(
                getEnv("GITHUB_REVIEW_LOG_URI"),
                getEnv("GITHUB_TOKEN"),
                getEnv("COMMIT_PROJECT"),
                getEnv("COMMIT_BRANCH"),
                getEnv("COMMIT_AUTHOR"),
                getEnv("COMMIT_MESSAGE")
        );

        /**
         * 项目：{{repo_name.DATA}} 分支：{{branch_name.DATA}} 作者：{{commit_author.DATA}} 说明：{{commit_message.DATA}}
         */
        WeiXin weiXin = new WeiXin(
                getEnv("WEIXIN_APPID"),
                getEnv("WEIXIN_SECRET"),
                getEnv("WEIXIN_TOUSER"),
                getEnv("WEIXIN_TEMPLATE_ID")
        );



        IOpenAI openAI = new ChatGLM(getEnv("CHATGLM_APIHOST"), getEnv("CHATGLM_APIKEYSECRET"));

        BaseGitOperation baseGitOperation = new GitRestAPIOperation(
                getGithubRequestUrl(),
                getEnv("GITHUB_TOKEN")
        );

        // 获取策略配置，如果未配置则默认为 remote 配置有commitComment、remote、或组合（remote,commitComment）
        String strategyType = System.getenv("CODE_REVIEW_TYPE");
        if (strategyType == null || strategyType.isEmpty()) {
            strategyType = "commitComment";
        }

        OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(baseGitOperation, gitCommand, openAI, weiXin, strategyType);
        openAiCodeReviewService.exec();

        logger.info("openai-code-review done!");
    }

    private static String getGithubRequestUrl() {
        String apiHost = "https://api.github.com";
        String repository = System.getenv("GITHUB_REPOSITORY");
        String eventName = System.getenv("GITHUB_EVENT_NAME");

        if (repository != null && !repository.isEmpty() && eventName != null && !eventName.isEmpty()) {
            //https://api.github.com/repos/username/repository/compare/main...feature-branch
            if ("pull_request".equals(eventName)) {
                String base = System.getenv("GITHUB_BASE_REF");
                String head = System.getenv("GITHUB_HEAD_REF");
                // PR 比较 API: /repos/{owner}/{repo}/compare/{base}...{head}
                return apiHost + "/repos/" + repository + "/compare/" + base + "..." + head;
            } else if ("push".equals(eventName)) {
                //https://api.github.com/repos/username/repository/commits/sha
                String sha = System.getenv("GITHUB_SHA");
                // Push Commit API: /repos/{owner}/{repo}/commits/{sha}
                return apiHost + "/repos/" + repository + "/commits/" + sha;
            }
        }

        // 兼容旧配置
        String checkCommitUrl = System.getenv("GITHUB_CHECK_COMMIT_URL");
        if (checkCommitUrl != null && !checkCommitUrl.isEmpty()) {
            return checkCommitUrl;
        }

        // 如果都无法获取，则抛出异常或返回 null 由后续处理（这里选择抛出异常提示配置）
        throw new RuntimeException("Cannot determine GitHub API URL. Please set GITHUB_REPOSITORY, GITHUB_EVENT_NAME etc. or GITHUB_CHECK_COMMIT_URL.");
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (null == value || value.isEmpty()) {
            throw new RuntimeException("value is null");
        }
        return value;
    }
}
