package com.chenyg.middleware.sdk.infrastructure.git;

public interface BaseGitOperation {

    /**
     * 定义一个获取变更内容的方法
     * @return 返回变更内容
     * @throws Exception
     */
    public String diff() throws Exception;
}
