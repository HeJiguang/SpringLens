package io.springlens.starter.probe;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @LensTool 修饰的接口如果有入参，给这个参数起一个明确的 JSON Key 名称，方便 AI 传值组装。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LensToolParam {

    /** AI 在调用工具时必须传进来的对应键名 */
    String value();
}
