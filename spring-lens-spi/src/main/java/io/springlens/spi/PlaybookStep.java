package io.springlens.spi;

/**
 * 排查步骤 (Playbook Step)。
 * 定义了解决特定问题剧本中的一个原子检查步骤。
 *
 * @param order    该步骤在剧本中的执行顺序（序号）。
 * @param title    该步骤的简短标题。
 * @param toolName 该步骤建议调用的诊断工具名称（需与某个 DiagnosticTool 中 descriptor().name() 匹配）。
 * @param guidance 给执行者（通常是 AI）的文字指导，例如如何理解该工具返回的结果以及下一步该去关注什么。
 */
public record PlaybookStep(
        int order,
        String title,
        String toolName,
        String guidance
) {
}
