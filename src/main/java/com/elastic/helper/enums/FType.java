package com.elastic.helper.enums;

public enum FType {
    /**
     * 字符串，默认类型。建议无特殊情况都用此类型
     */
    STRING,
    /**
     * 日期类型 #号分隔 格式要求：2016#2019
     */
    DATE,
    /**
     * 多值类型，逗号分隔  格式要求：a,b,c
     */
    ARRAY,

    /**
     * 查询的字段内容是否为空 true：不为空。 false：为空
     */
    EXISTS,

    /**
     * 内容不查询
     */
    MUST_NOT;

}
