package com.elastic.helper;

import com.elastic.helper.tools.ElasticSearchHelper;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ElasticApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticApplication.class, args);
    }


    @Bean("searchHelper")
    public ElasticSearchHelper elasticSearchHelper(@Autowired RestHighLevelClient highLevelClient){
        return new ElasticSearchHelper(highLevelClient);
    }
}
