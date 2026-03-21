package io.springlens.model;

import io.springlens.model.diagnostic.ProbeCapturePhase;

/**
 * 探针描述符 (Probe Descriptor)。
 * 用于向服务端和外部 UI 声明应用程序内部目前埋设了哪些 @LensWatch 探针。
 * 注意：这仅仅是探针的“静态结构声明”，不包含具体抓取到的值。
 *
 * @param probeId       探针声明的唯一 ID。
 * @param description   开发人员为探针撰写的功能说明。
 * @param captureSource 该探针所在的类名或具体方法源（如 OrderController.lookup）。
 * @param phase         该探针被设计用来抓取哪个阶段的数据。
 */
public record ProbeDescriptor(
        String probeId,
        String description,
        String captureSource,
        ProbeCapturePhase phase
) {
}
