package com.chenyg.middleware.sdk.infrastructure.git.dto;


import lombok.Data;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-29  15:34
 */
@Data
public class SingleCommitResponseDTO {

    /**
     * commit sha
     */
    private String sha;

    /**
     * commit html url
     */
    private String html_url;

    /**
     * commit
     */
    private Commit commit;

    private CommitFile[] files;

    @Data
    public static class Commit {
        private String message;
    }

    @Data
    public static class CommitFile {
        private String filename;
        private String raw_url;
        private String patch;
    }
}
