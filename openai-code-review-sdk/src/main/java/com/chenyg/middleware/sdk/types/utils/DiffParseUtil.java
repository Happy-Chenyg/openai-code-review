package com.chenyg.middleware.sdk.types.utils;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-29  14:56
 */
public class DiffParseUtil {

    /**
     * 获取文件最后修改的位置
     * @param fileDiff 文件变更的字符串
     * @return 行号
     */
    public static int parseLastDiffPosition(String fileDiff) {

        // 暂时先用最大一个位置，未来看看是否用+号开始的行进行处理
        String[] lines = fileDiff.split("\n");
        return lines.length-1;
    }
}
