package com.elastic.helper.annotation;

import com.elastic.helper.enums.FType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FilterType {

    /**
     * 过滤类型
     * @see FType
     * @return
     */
    FType value() default FType.STRING;

    /**
     * 忽略过滤 如果设置此值，为此值则跳过
     * @return
     */
    String ignoreValue() default "";


}
