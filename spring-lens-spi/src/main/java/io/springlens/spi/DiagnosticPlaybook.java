package io.springlens.spi;

import java.util.List;

/**
 * 诊断剧本 (Diagnostic Playbook)。
 * 这是一个高级抽象，用于指引 AI Agent 遇到某种已知特定问题时，应该按什么步骤（标准操作程序/SOP）去排查。
 */
public interface DiagnosticPlaybook {

    /**
     * @return 剧本的唯一标识符（如 "exception-triage"）。
     */
    String id();

    /**
     * @return 描述本剧本可以用来排查哪一类问题或触发场景。
     */
    String problem();

    /**
     * @return 剧本中包含的有序排查步骤列表。AI 将按照此列表上的指示和建议依次执行具体的诊断工具。
     */
    List<PlaybookStep> steps();
}
