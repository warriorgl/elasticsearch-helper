package com.elastic.helper.tools;

import com.elastic.helper.annotation.FilterType;
import com.elastic.helper.enums.FType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Slf4j
public class ESFilterWrapper implements Serializable {


    public <E> List<QueryBuilder> filter(E filterObj) {
        try {
            List<QueryBuilder> filterList = new ArrayList<>();
            Field[] fields = filterObj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String value = (String) field.get(filterObj);
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                FilterType filterType = field.getAnnotation(FilterType.class);
                if (!filterType.exist()){ //忽略字段
                    continue;
                }
                /*获取过滤类型*/
                FType current = filterType != null ? filterType.value() : FType.STRING;
                /*处理过滤别名*/
                String fieldName = field.getName();
                if (filterType != null && !"".equals(filterType.alias())) {
                    fieldName = filterType.alias();
                }
                /*处理过滤 特殊值*/
                String ignoreValue = filterType == null ? "" : filterType.ignoreValue();
                if (ignoreValue.equals(value)) {
                    continue;
                }
                if (current == FType.STRING) {
                    filterList.add(QueryBuilders.termQuery(fieldName, value));
                } else if (current == FType.DATE) {
                    buildDateQuery(filterList, value, filterType, fieldName);
                } else if (current == FType.ARRAY) {
                    buildArrayQuery(filterList, value, filterType, fieldName);
                } else if (current == FType.EXISTS) {
                    buildExistsQuery(filterList, value, fieldName);
                } else if (current == FType.MUST_NOT){
                    buildMustNotQuery(filterList, value, filterType, fieldName);
                }
            }
            return filterList;
        } catch (IllegalAccessException i) {
            log.error("filterwrapper exception:{}", i.getLocalizedMessage());
        }
        return Collections.EMPTY_LIST;
    }

    private void buildMustNotQuery(List<QueryBuilder> filterList, String value, FilterType filterType, String fieldName) {
        String[] array = value.split(filterType.separator());
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String a : array) {
            boolQueryBuilder.mustNot(QueryBuilders.termQuery(fieldName, a));
        }
        filterList.add(boolQueryBuilder);
    }


    private void buildExistsQuery(List<QueryBuilder> filterList, String value, String fieldName) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolean bvalue = Boolean.valueOf(value);
        if (bvalue) {
            filterList.add(QueryBuilders.existsQuery(fieldName));
        } else {
            boolQueryBuilder.mustNot(QueryBuilders.existsQuery(fieldName));
            filterList.add(boolQueryBuilder);
        }
    }

    private void buildArrayQuery(List<QueryBuilder> filterList, String value, FilterType filterType, String fieldName) {
        String[] array = value.split(filterType.separator());
        String finalFieldName = fieldName;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String a : array) {
            boolQueryBuilder.should(QueryBuilders.termQuery(finalFieldName, a));
        }
        filterList.add(boolQueryBuilder);
    }

    private void buildDateQuery(List<QueryBuilder> filterList, String value, FilterType filterType, String fieldName) {
        String[] dateRange = value.split(filterType.separator());
        if (value.startsWith(filterType.separator())) {
            filterList.add(QueryBuilders.rangeQuery(fieldName).to(dateRange[1]));
        } else if (value.endsWith(filterType.separator())) {
            filterList.add(QueryBuilders.rangeQuery(fieldName).from(dateRange[0]));
        } else {
            filterList.add(QueryBuilders.rangeQuery(fieldName).from(dateRange[0]).to(dateRange[1]));
        }
    }

}
