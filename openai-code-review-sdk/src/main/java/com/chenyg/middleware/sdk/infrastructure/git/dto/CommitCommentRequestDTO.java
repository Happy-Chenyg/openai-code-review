package com.chenyg.middleware.sdk.infrastructure.git.dto;

import lombok.Data;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-30  14:14
 */
@Data
public class CommitCommentRequestDTO {

    /**
     * 评论内容
     */
    private String body;

    /**
     * 文件的相对路径
     */
    private String path;

    /**
     * 要评论的行号位置
     */
    private int position;
}
