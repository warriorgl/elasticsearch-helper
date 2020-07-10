package com.elastic.helper.demo;

import com.elastic.helper.tools.ElasticSearchHelper;
import com.elastic.helper.tools.ESFilterWrapper;
import com.elastic.helper.tools.ESLambdaWrapper;
import com.elastic.helper.tools.SearchResult;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 示例
 */
@RestController
public class DemoController {


    @Autowired
    private ElasticSearchHelper searchHelper;

    /**
     * 检索示例
     * @param queryBean
     * @param filterBean
     * @return
     */
    public SearchResult keyword(QueryBean queryBean,FilterBean filterBean){
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        /*过滤*/
        List<QueryBuilder> filterList = boolQueryBuilder.filter();
        filterList.addAll(new ESFilterWrapper().filter(filterBean));

        List<QueryBuilder> boolmustList = boolQueryBuilder.must();
        /*查询*/
        boolmustList.add(QueryBuilders.matchQuery("title",queryBean.getKeyword()));
        
        searchSourceBuilder.query(boolQueryBuilder);
        
        return requestResult(queryBean,searchSourceBuilder);
    }


    /**
     * 单条更新示例
     * @throws IOException
     */
    public void lambdaUpdateDocument(){
        ESBean bean= new ESBean();
        bean.setCode("1");
        bean.setName("2");
        ESLambdaWrapper<ESBean> lambdaWrapper= new ESLambdaWrapper();
        lambdaWrapper.docId("1").add(ESBean::getCode,"1").add(ESBean::getName,"2");
        searchHelper.lambdaUpdateDocument(lambdaWrapper);
    }


    /**
     * 单条创建示例
     */
    public void lambdaCreateDocument(){
        ESBean bean= new ESBean();
        bean.setCode("1");
        bean.setName("2");
        ESLambdaWrapper<ESBean> lambdaWrapper= new ESLambdaWrapper();
        lambdaWrapper.docId("1").add(ESBean::getCode,"1").add(ESBean::getName,"2");
        searchHelper.lambdaCreateDocument(lambdaWrapper);
    }


    /**
     * 删除
     * @throws IOException
     */
    public void delete() throws IOException {
        searchHelper.deleteDocument(new DeleteRequest().id("1"));
    }

    /**
     * 批量操作
     */
    public void bulkDocument(){
        List<DocWriteRequest<?>> requests = new ArrayList<>();
    }


    private SearchResult requestResult(QueryBean queryBean,SearchSourceBuilder searchSourceBuilder) {
        SearchRequest firstSearchRequest = new SearchRequest("indexName");
        searchSourceBuilder.trackTotalHits(true); //设置返回总数
        searchSourceBuilder.trackScores(true);
        searchSourceBuilder.size(queryBean.getSize()).from((queryBean.getPage()-1)*queryBean.getSize());
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        /*返回字段*/
        searchSourceBuilder.fetchSource(new String[]{"title"}, Strings.EMPTY_ARRAY);
        /*排序*/
        searchSourceBuilder.sort(new FieldSortBuilder(queryBean.getSortField()).order(queryBean.getSortOrder()));
        /*学科聚合*/
        searchSourceBuilder.aggregation(AggregationBuilders.terms("suject_count").field("suject_code").size(100000).order(BucketOrder.key(true)));
        firstSearchRequest.source(searchSourceBuilder);
        SearchResult searchResult = searchHelper.searchDocument(firstSearchRequest);
        return searchResult;
    }


    @Data
    public class ESBean {
        private String name;

        private String code;
    }


}
