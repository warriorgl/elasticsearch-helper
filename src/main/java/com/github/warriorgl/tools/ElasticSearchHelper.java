package com.github.warriorgl.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ScrollableHitSource;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import java.io.IOException;
import java.util.*;

@Slf4j
public class ElasticSearchHelper {


    private RestHighLevelClient highLevelClient;


    public ElasticSearchHelper(RestHighLevelClient highLevelClient) {
        this.highLevelClient = highLevelClient;
    }


    /**
     * 批量处理数据
     * DocWriteRequest？
     * ->IndexRequest
     * ->UpdateRequest
     * ->DeleteRequest
     *
     * @param requests
     * @return
     */
    public BulkResponse bulkDocument(List<DocWriteRequest<?>> requests) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        for (DocWriteRequest writeRequest : requests) {
            bulkRequest.add(writeRequest);
        }
        return highLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }


    public void indexDocument(IndexRequest indexRequest) throws IOException {
        highLevelClient.index(indexRequest,RequestOptions.DEFAULT);
    }

    /**
     * 异步批量数据处理
     * DocWriteRequest？
     * ->IndexRequest
     * ->UpdateRequest
     * ->DeleteRequest
     *
     * @param requests
     */
    public void bulkAsyncDocument(List<DocWriteRequest<?>> requests) {
        bulkAsyncListenerDocument(requests, actionListener());
    }

    /**
     * 异步批量数据处理-自定义响应
     *
     * @param requests
     * @param actionListener
     */
    public void bulkAsyncListenerDocument(List<DocWriteRequest<?>> requests, ActionListener<BulkResponse> actionListener) {
        BulkRequest bulkRequest = new BulkRequest();
        for (DocWriteRequest writeRequest : requests) {
            bulkRequest.add(writeRequest);
        }
        highLevelClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, actionListener);
    }


    private ActionListener<BulkResponse> actionListener() {
        ActionListener<BulkResponse> listener = new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkResponse) {
                if (bulkResponse.hasFailures()) {
                    log.error("Increased resource failure causes：{}", bulkResponse.buildFailureMessage());
                }
            }

            @Override
            public void onFailure(Exception e) {
                log.error("Asynchronous batch increases data exceptions：{}", e.getLocalizedMessage());
            }
        };
        return listener;
    }


    /**
     * 检索
     *
     * @param searchRequest
     * @return
     */
    public SearchResult searchDocument(SearchRequest searchRequest) {
        List<Map<String, Object>> list = new ArrayList<>();
        SearchResponse searchResponse;
        try {
            searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] searchHit = searchResponse.getHits().getHits();
            searchResponse.getTook().getMillis();
            long totalHits = searchResponse.getHits().getTotalHits().value;
            long took = searchResponse.getTook().getMillis();
            for (SearchHit document : searchHit) {
                Map<String, Object> item = document.getSourceAsMap();
                if (item == null) {
                    continue;
                }
                Map<String, HighlightField> highlightFields = document.getHighlightFields();
                if (!highlightFields.isEmpty()) {
                    for (String key : highlightFields.keySet()) {
                        Text[] fragments = highlightFields.get(key).fragments();
                        if (item.containsKey(key)) {
                            item.put(key, fragments[0].string());
                        }
                        String[] fieldArray = key.split("[.]");
                        if (fieldArray.length > 1) {
                            item.put(fieldArray[0], fragments[0].string());
                        }
                    }
                }
                list.add(item);
            }
            Map<String, Map<String, Long>> aggregations = getAggregation(searchResponse.getAggregations());
            return new SearchResult(totalHits, list, took, aggregations);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new SearchResult();
    }


    private Map<String, Map<String, Long>> getAggregation(Aggregations aggregations) {
        if (aggregations == null) {
            return Collections.EMPTY_MAP;
        }
        Map<String, Map<String, Long>> result = new HashMap<>();
        Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
        aggregationMap.forEach((k, v) -> {
            Map<String, Long> agg = new HashMap<>();
            List<? extends Terms.Bucket> buckets = ((ParsedStringTerms) v).getBuckets();
            for (Terms.Bucket bucket : buckets) {
                agg.put(bucket.getKeyAsString(), bucket.getDocCount());
            }
            result.put(k, agg);
        });
        return result;
    }


    /**
     * 删除
     *
     * @param request
     * @return
     */
    public Boolean deleteDocument(DeleteRequest request) throws IOException {
        DeleteResponse deleteResponse = highLevelClient.delete(request, RequestOptions.DEFAULT);
        if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
            log.info("not found doc id:{}", deleteResponse.getId());
            return false;
        }
        if (deleteResponse.getResult() == DocWriteResponse.Result.DELETED) {
            return true;
        }
        log.info("deleteResponse Status:{}", deleteResponse.status());
        return false;
    }


    /**
     * 异步查询更新
     *
     * @param request
     */
    public void updateByQueryDocument(UpdateByQueryRequest request) {
        try {
            highLevelClient.updateByQuery(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("updateByQuery Exception {}", e.getLocalizedMessage());
        }
    }


    private ActionListener<BulkByScrollResponse> bulkByScrolllistener() {
        return new ActionListener<BulkByScrollResponse>() {
            @Override
            public void onResponse(BulkByScrollResponse bulkResponse) {
                List<BulkItemResponse.Failure> failures = bulkResponse.getBulkFailures();
                if (!failures.isEmpty()) {
                    log.error("BulkByScrollResponse failures:{}", StringUtils.join(failures, "@"));
                }
                List<ScrollableHitSource.SearchFailure> searchFailures = bulkResponse.getSearchFailures();
                if (!failures.isEmpty()) {
                    log.error("BulkByScrollResponse searchFailures:{}", StringUtils.join(searchFailures, "@@"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                log.error("BulkByScrollResponse Exceptions：{}", e.getLocalizedMessage());
            }
        };
    }

    /**
     * 个数查询
     *
     * @param countRequest
     * @return
     */
    public Long countDocument(CountRequest countRequest) {
        try {
            CountResponse countResponse = highLevelClient.count(countRequest, RequestOptions.DEFAULT);
            return countResponse.getCount();
        } catch (IOException e) {
            log.error("CountResponse Exceptions：{}", e.getLocalizedMessage());
            return 0L;
        }
    }


    public boolean updateDocument(UpdateRequest updateRequest) {
        try {
            highLevelClient.update(updateRequest, RequestOptions.DEFAULT);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("UpdateRequest Exception:{}", e.getLocalizedMessage());
            return false;
        }
    }


    public boolean lambdaUpdateDocument(ESLambdaWrapper<?> updateWrapper){
        if (StringUtils.isBlank(updateWrapper.getDocId())) {
            log.warn("id does not exist:{}",updateWrapper.getDocId());
            return false;
        }
        if (updateWrapper.getFieldMap().isEmpty()) {
            log.warn("content is null");
            return false;
        }
        try {
            UpdateRequest request = new UpdateRequest(updateWrapper.getIndex(), updateWrapper.getDocId());
            request.doc(updateWrapper.getFieldMap());
            highLevelClient.update(request, RequestOptions.DEFAULT);
        }catch (IOException io){
            log.error("lambdaUpdateDocument Exception:{}",io.getMessage());
            return false;
        }
        return true;
    }


    public boolean lambdaCreateDocument(ESLambdaWrapper<?> indexWrapper){
        if (indexWrapper.getFieldMap().isEmpty()) {
            log.warn("content is null");
            return false;
        }
        try {
            IndexRequest request = new IndexRequest(indexWrapper.getIndex()).id(indexWrapper.getDocId());
            request.source(indexWrapper.getFieldMap(), XContentType.JSON);
            highLevelClient.index(request, RequestOptions.DEFAULT);
        }catch (IOException io){
            log.error("lambdaCreateDocument Exception:{}",io.getMessage());
            return false;
        }
        return true;
    }


}
