package com.chenyg.middleware.sdk.infrastructure.git;

public interface BaseGitOperation {

    /**
     * 定义一个获取变更内容的方法
     * @return 返回变更内容
     * @throws Exception
     */
    public String diff() throws Exception;

    /**
     * 获取变更内容并保存
     * @param result 评审结果
     * @return 跳转地址
     * @throws Exception
     */
    public String writeResult(String result) throws Exception;
}
