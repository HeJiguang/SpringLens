package io.springlens.starter.probe;

import io.springlens.model.diagnostic.ProbeCapturePhase;
import io.springlens.model.ProbeDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

/**
 * 探针名册 (Probe Registry)。
 * 不光管运行期的抓取，Spring Lens 会在启动时全局扫一次雷！
 * 搜罗出在代码里被标注过的所有可用探针清单，用于生成一本说明书 (`/internal/spring-lens/probes` 接口暴露)。
 * 这样 AI 大模型一看这本说明书，就知道这个微服务目前布置了哪些哨兵雷达。
 */
public class LensProbeRegistry {

    private final Map<String, ProbeDescriptor> probes = new ConcurrentHashMap<>();

    public LensProbeRegistry(ApplicationContext applicationContext) {
        // 利用 Spring 开机时全局扫包机制，一次性找出所有的埋点
        applicationContext.getBeansOfType(Object.class).values().forEach(bean -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getMethods()) {
                LensWatch watch = method.getAnnotation(LensWatch.class);
                if (watch != null) {
                    registerAnnotation(watch);
                }
            }
        });
    }

    public ProbeDescriptor registerAnnotation(LensWatch watch) {
        return probes.computeIfAbsent(watch.id(), ignored -> new ProbeDescriptor(
                watch.id(),
                watch.description(),
                "annotation",
                ProbeCapturePhase.of(watch.phase())
        ));
    }

    // 手动调用 Lens.look() 也会在这里现场注册，相当于隐式登记！
    public ProbeDescriptor registerManual(String id, String description) {
        return probes.computeIfAbsent(id, ignored -> new ProbeDescriptor(
                id,
                description,
                "manual",
                ProbeCapturePhase.MANUAL
        ));
    }

    public List<ProbeDescriptor> list() {
        return probes.values().stream().sorted((left, right) -> left.probeId().compareTo(right.probeId())).toList();
    }
}
