package com.chenyg.middleware.sdk.infrastructure.git.write.impl;

import com.chenyg.middleware.sdk.infrastructure.git.BaseGitOperation;
import com.chenyg.middleware.sdk.infrastructure.git.GitCommand;
import com.chenyg.middleware.sdk.infrastructure.git.write.IWriteHandlerStrategy;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-30  14:44
 */
public class CommitCommentWriteHandlerStrategy implements IWriteHandlerStrategy {
    private BaseGitOperation baseGitOperation;

    @Override
    public String typeName() {

        return "commitComment";
    }

    @Override
    public void initData(BaseGitOperation gitOperation) {
        baseGitOperation = gitOperation;
    }

    @Override
    public String execute(String codeResult) throws Exception {
        return baseGitOperation.writeResult(codeResult);
    }

}
