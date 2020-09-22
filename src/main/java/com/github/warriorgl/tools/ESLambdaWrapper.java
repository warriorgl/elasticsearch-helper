package com.github.warriorgl.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ESLambdaWrapper<T> implements Serializable {

    private Map<String,Object> fieldMap =new HashMap<>();

    private String docId;

    private String index;

    public ESLambdaWrapper<T> add(ESFunction<T, ?> lambda, Object object){
        String fieldName=SerializedLambdaUtils.convertToFieldName(lambda);
        fieldMap.put(fieldName,object);
        return this;
    }


    public ESLambdaWrapper<T> docId(String id){
        this.docId=id;
        return this;
    }

    public ESLambdaWrapper<T> index(String index){
        this.index=index;
        return this;
    }


    public String getIndex() {
        return index;
    }

    public String getDocId() {
        return docId;
    }

    public Map<String, Object> getFieldMap() {
        return fieldMap;
    }



}
