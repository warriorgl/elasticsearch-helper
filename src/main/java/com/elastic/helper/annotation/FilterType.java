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


    /**
     * 过滤别名 如果一个字段检索条件不一样可使用
     * @return
     */
    String alias() default "";

    /**
     * 多个数据分隔符，默认逗号
     * @return
     */
    String separator() default ",";


    /**
     * 是否存在此条件 默认true
     * @return
     */
    boolean exist() default true;


}
