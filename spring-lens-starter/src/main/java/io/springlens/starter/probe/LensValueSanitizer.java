package io.springlens.starter.probe;

import java.util.Map;
import tools.jackson.databind.ObjectMapper;

/**
 * 探针数据洗消器脱敏器 (Value Sanitizer)。
 * 这是为了防止应用产生的局部变量（如大型集合、循环引用的 ORM 对象）在捕捉后引发灾难性内存泄漏的防护层。
 * 它尝试使用 ObjectMapper 把真实变量转换成松散的无引用依赖字典树 (Map 等)。
 */
public class LensValueSanitizer {

    private final ObjectMapper objectMapper;

    public LensValueSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SanitizedValue sanitize(Object value) {
        if (value == null) {
            return new SanitizedValue(null, "null");
        }
        Object normalized;
        try {
            // 利用 Jackson 把对象压平变成 Json 的 Map 结构！破坏一切危险的死循环嵌套引用
            normalized = objectMapper.convertValue(value, Object.class);
        }
        catch (IllegalArgumentException ex) {
            // 压平失败，那对不住了只能让你变成一行长长的字符串了
            normalized = String.valueOf(value);
        }
        
        // 长度截断保护：太长的值对于 AI 和画图都没有多大意义，还容易炸 JVM
        if (normalized instanceof String text && text.length() > 512) {
            normalized = text.substring(0, 512);
        }
        return new SanitizedValue(normalized, value.getClass().getName());
    }

    public record SanitizedValue(Object value, String valueType) {
    }
}
