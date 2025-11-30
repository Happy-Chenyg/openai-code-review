package com.chenyg.middleware.sdk.infrastructure.git.write;

import com.chenyg.middleware.sdk.infrastructure.git.BaseGitOperation;

/**
 * @author Chenyg
 * @return
 */
public interface IWriteHandlerStrategy {

    /**
     * 获取类型名称
     * @return
     */
    public String typeName();

    /**
     * 初始化数据
     * @param gitOperation git操作
     */
    public void initData(BaseGitOperation gitOperation);

    /**
     * 执行写入处理策略
     * @param codeResult 待处理的代码结果
     * @return 调整url
     */
    public String execute(String codeResult) throws Exception;
}
