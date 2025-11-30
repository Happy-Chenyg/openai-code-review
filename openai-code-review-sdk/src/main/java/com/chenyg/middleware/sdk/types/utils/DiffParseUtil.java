package com.chenyg.middleware.sdk.types.utils;

/**
 * @author Chenyg
 * @desc
 * @since 2025-11-29  14:56
 */
public class DiffParseUtil {

    /**
     * 获取文件最后修改的位置
     * <p>
     *     优先寻找最后一行以+号开头的代码行（新增行），
     *     如果找不到，则返回最后一行。
     * </p>
     * @param fileDiff 文件变更的字符串
     * @return 行号
     */
    public static int parseLastDiffPosition(String fileDiff) {
        String[] lines = fileDiff.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].startsWith("+") && !lines[i].startsWith("+++")) {
                return i;
            }
        }
        return lines.length - 1;
    }
}
