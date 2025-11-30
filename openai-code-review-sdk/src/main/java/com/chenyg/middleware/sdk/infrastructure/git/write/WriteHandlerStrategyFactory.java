package com.chenyg.middleware.sdk.infrastructure.git.write;

import com.chenyg.middleware.sdk.infrastructure.git.write.impl.CommitCommentWriteHandlerStrategy;
import com.chenyg.middleware.sdk.infrastructure.git.write.impl.RemoteRepositoryWriteHandlerStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-30  14:46
 */
public class WriteHandlerStrategyFactory {

    private static Map<String, IWriteHandlerStrategy> registry = new HashMap<>();

    // 静态初始化块，会自动将两个策略实现类注册到 registry 映射表中
    static {
        registry.put("commitComment", new CommitCommentWriteHandlerStrategy());
        registry.put("remote", new RemoteRepositoryWriteHandlerStrategy());
    }

    public static IWriteHandlerStrategy getStrategy(String typeName) {
        return registry.get(typeName);
    }
}
