package com.github.warriorgl.demo;


import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;

@Data
public class QueryBean {

    private String keyword;

    private String sortField;

    private String sort = "asc";

    private Integer size=20;

    private Integer page=1;


    public SortOrder getSortOrder(){
        if (StringUtils.lowerCase(this.sort).equals("desc")){
            return SortOrder.DESC;
        }
        return SortOrder.ASC;
    }
}
