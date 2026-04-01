package io.springlens.starter.capability;

import jakarta.annotation.PreDestroy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.ReflectionUtils;

public class RuntimeSafetyInspector {

    private static final Set<Class<?>> NON_THREAD_SAFE_COLLECTION_TYPES = Set.of(
            ArrayList.class,
            LinkedList.class,
            ArrayDeque.class,
            HashMap.class,
            LinkedHashMap.class,
            TreeMap.class,
            HashSet.class,
            LinkedHashSet.class,
            TreeSet.class
    );

    private final ApplicationContext applicationContext;

    public RuntimeSafetyInspector(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public RuntimeSafetyInspectionReport inspect(int maxFindings) {
        int normalizedMaxFindings = maxFindings <= 0 ? 20 : maxFindings;
        List<RuntimeSafetyFinding> findings = new ArrayList<>();
        int inspectedBeanCount = 0;

        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (findings.size() >= normalizedMaxFindings) {
                break;
            }
            Class<?> beanType = applicationContext.getType(beanName);
            if (!isCandidateApplicationBean(beanType) || !applicationContext.isSingleton(beanName)) {
                continue;
            }
            Object bean = safeGetBean(beanName);
            if (bean == null) {
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (!isCandidateApplicationBean(targetClass)) {
                continue;
            }
            inspectedBeanCount++;
            inspectBean(beanName, bean, targetClass, findings, normalizedMaxFindings);
        }

        return new RuntimeSafetyInspectionReport(RuntimeSafetyCapability.CAPABILITY_ID, inspectedBeanCount, findings);
    }

    private void inspectBean(
            String beanName,
            Object bean,
            Class<?> targetClass,
            List<RuntimeSafetyFinding> findings,
            int maxFindings
    ) {
        boolean hasExecutorShutdownHook = hasExecutorShutdownHook(bean, targetClass);
        boolean hasAsyncMethod = hasAsyncMethod(targetClass);
        ReflectionUtils.doWithFields(targetClass, field -> {
            if (findings.size() >= maxFindings) {
                return;
            }
            inspectField(beanName, bean, targetClass, field, hasExecutorShutdownHook, hasAsyncMethod, findings);
        }, field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()));
    }

    private void inspectField(
            String beanName,
            Object bean,
            Class<?> targetClass,
            Field field,
            boolean hasExecutorShutdownHook,
            boolean hasAsyncMethod,
            List<RuntimeSafetyFinding> findings
    ) {
        ReflectionUtils.makeAccessible(field);
        Object fieldValue = ReflectionUtils.getField(field, bean);
        Class<?> fieldType = field.getType();
        Class<?> effectiveType = fieldValue != null ? fieldValue.getClass() : fieldType;

        if (ThreadLocal.class.isAssignableFrom(fieldType)) {
            findings.add(finding(
                    "singleton-thread-local-field",
                    "high",
                    beanName,
                    targetClass,
                    field,
                    effectiveType,
                    "Singleton bean keeps ThreadLocal state, which can leak request data across pooled threads.",
                    "Move this state to method scope or request scope, or add explicit cleanup on every execution path."
            ));
            if (hasAsyncMethod) {
                findings.add(finding(
                        "singleton-async-threadlocal-context-risk",
                        "high",
                        beanName,
                        targetClass,
                        field,
                        effectiveType,
                        "Singleton bean combines @Async execution with ThreadLocal state, which usually breaks context propagation across threads.",
                        "Avoid ThreadLocal state on async beans, or propagate and clear context explicitly at async boundaries."
                ));
            }
        }

        if ((ExecutorService.class.isAssignableFrom(fieldType) || ScheduledExecutorService.class.isAssignableFrom(fieldType))
                && !hasExecutorShutdownHook) {
            findings.add(finding(
                    "singleton-manual-executor-field",
                    "high",
                    beanName,
                    targetClass,
                    field,
                    effectiveType,
                    "Singleton bean owns a manual executor without an explicit shutdown hook, which risks thread and memory retention.",
                    "Add @PreDestroy shutdown logic or delegate executor lifecycle to Spring-managed infrastructure."
            ));
        }

        if (isNonThreadSafeCollection(effectiveType)) {
            findings.add(finding(
                    "singleton-non-thread-safe-collection",
                    "high",
                    beanName,
                    targetClass,
                    field,
                    effectiveType,
                    "Singleton bean exposes shared mutable collection state backed by a non-thread-safe implementation.",
                    "Replace it with a concurrent collection, confine it to method scope, or guard it with explicit synchronization."
            ));
        }

        if (isThreadUnsafeFormatter(effectiveType)) {
            findings.add(finding(
                    "singleton-thread-unsafe-formatter",
                    "high",
                    beanName,
                    targetClass,
                    field,
                    effectiveType,
                    "Singleton bean stores a thread-unsafe formatter instance that can corrupt parsing or formatting under concurrency.",
                    "Replace it with java.time formatters or create formatter instances per call."
            ));
        }

        if (isUnboundedQueue(fieldValue)) {
            findings.add(finding(
                    "singleton-unbounded-queue-field",
                    "high",
                    beanName,
                    targetClass,
                    field,
                    effectiveType,
                    "Singleton bean keeps an unbounded queue, which can grow without backpressure and retain memory under load.",
                    "Use a bounded queue with rejection/backpressure or move buffering to infrastructure that enforces capacity."
            ));
        }

        if (isNonAtomicCounterField(field)) {
            findings.add(finding(
                    "singleton-non-atomic-counter-field",
                    "medium",
                    beanName,
                    targetClass,
                    field,
                    effectiveType,
                    "Singleton bean exposes a mutable counter-like field without volatile or atomic coordination.",
                    "Replace it with AtomicInteger/AtomicLong/LongAdder or move the counter to a confined scope."
            ));
        }
    }

    private boolean hasExecutorShutdownHook(Object bean, Class<?> targetClass) {
        if (bean instanceof AutoCloseable || bean instanceof DisposableBean) {
            return true;
        }
        return Arrays.stream(targetClass.getDeclaredMethods())
                .anyMatch(this::hasPreDestroyAnnotation);
    }

    private boolean hasPreDestroyAnnotation(Method method) {
        return method.isAnnotationPresent(PreDestroy.class);
    }

    private boolean hasAsyncMethod(Class<?> targetClass) {
        return Arrays.stream(targetClass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Async.class));
    }

    private boolean isNonThreadSafeCollection(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (!Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)) {
            return false;
        }
        return NON_THREAD_SAFE_COLLECTION_TYPES.stream().anyMatch(candidate -> candidate.isAssignableFrom(type));
    }

    private boolean isThreadUnsafeFormatter(Class<?> type) {
        if (type == null) {
            return false;
        }
        return SimpleDateFormat.class.isAssignableFrom(type)
                || DateFormat.class.equals(type)
                || Calendar.class.isAssignableFrom(type);
    }

    private boolean isUnboundedQueue(Object value) {
        if (!(value instanceof BlockingQueue<?> queue)) {
            return false;
        }
        return queue instanceof LinkedBlockingQueue<?> && queue.remainingCapacity() == Integer.MAX_VALUE;
    }

    private boolean isNonAtomicCounterField(Field field) {
        if (Modifier.isFinal(field.getModifiers()) || Modifier.isVolatile(field.getModifiers())) {
            return false;
        }
        if (!isCounterLikeName(field.getName())) {
            return false;
        }
        Class<?> type = field.getType();
        return int.class.equals(type)
                || long.class.equals(type)
                || Integer.class.equals(type)
                || Long.class.equals(type);
    }

    private boolean isCounterLikeName(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase();
        return normalized.contains("count")
                || normalized.contains("counter")
                || normalized.contains("sequence")
                || normalized.contains("index")
                || normalized.contains("version");
    }

    private RuntimeSafetyFinding finding(
            String ruleId,
            String severity,
            String beanName,
            Class<?> targetClass,
            Field field,
            Class<?> effectiveType,
            String message,
            String recommendation
    ) {
        return new RuntimeSafetyFinding(
                ruleId,
                severity,
                beanName,
                targetClass.getName(),
                field.getName(),
                effectiveType.getName(),
                message,
                recommendation
        );
    }

    private Object safeGetBean(String beanName) {
        try {
            return applicationContext.getBean(beanName);
        }
        catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isCandidateApplicationBean(Class<?> beanType) {
        if (beanType == null) {
            return false;
        }
        String className = beanType.getName();
        return !className.startsWith("java.")
                && !className.startsWith("jakarta.")
                && !className.startsWith("org.springframework.")
                && !className.startsWith("io.springlens.agent.")
                && !className.startsWith("io.springlens.model.")
                && !className.startsWith("io.springlens.runtime.")
                && !className.startsWith("io.springlens.server.")
                && !className.startsWith("io.springlens.spi.")
                && !className.startsWith("io.springlens.starter.");
    }
}
