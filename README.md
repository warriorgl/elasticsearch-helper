### # Elasticsearch-Helper
-- QQ群：199016700--

### 简介

Elasticsearch-Helper 是一个Java对 Elasticsearch RHLC API的封装工具，通过封装简化官方API写法，降低相关API的学习成本，减少代码编写工作，提高工作效率。  

Elasticsearch-Helper当前版本主要简化了对各种格式数据的过滤操作。通过封装常用数据类型的过滤，通过添加注解即可根据某个字段进行过滤，来达到减少代码量的目的。

项目使用springboot框架创建。所以JDK版本需要8以上。

### 项目依赖版本说明
     Java版本：1.8
     ES版本：7.2.1
### 使用说明
1. 以Module方式加入已有项目中。删掉配置文件中的配置application.yml，在你的主项目配置文件中配置ES节点  elasticsearch.nodes。详见配置文件配置方式
2. 创建帮助类实例

```
    @Bean("searchHelper")
    public ElasticSearchHelper elasticSearchHelper(@Autowired RestHighLevelClient highLevelClient){
        return new ElasticSearchHelper(highLevelClient);
    }
```

3. 在其他类中注入 searchHelper 即可。

### 注解说明

已FilterBean.java类为例讲解

@FilterType注解的参数默认是字符串类型。注意属性类型不要定义成其他类型。因为给ES传值并不需要类型转换，所以统一使用字符串。

@FilterType注解说明

| 名称    | 说明              |
|-------|-----------------|
| value | 标注此值是什么类型，默认字符串；具体支持类型请查看@FType注解 |
| ignoreValue| 忽略过滤值，例如ignoreValue = "0" 则ES中为0的数据就不过滤了。适用于想过滤某些数据但是还要保留部分数据。还有默认值的情况。 |
| alias | 设置别名，适用于一个字段要进行不同的过滤操作，那么就可以创建多个字段把此值设置成想过滤的字段。|
| separator | 分隔符，传多个数据分隔符，默认“,” |
| exist | 是否当做过滤条件，默认ture |

@FType注解说明
| 名称 | 说明 |
|----|----|
| STRING | 字符串，默认类型。建议无特殊情况都用此类型  |
| DATE   | 日期类型 默认#号分隔 格式要求：2016#2019 可以通过设置 FilterType注解属性separator来改变分隔符 |
| ARRAY  | 值类型，默认逗号分隔  格式要求：a,b,c  可以通过设置 FilterType注解属性separator来改变分隔符 |
| EXISTS | 查询的字段是否存在 true：不为空。 false：为空  |
| MUST_NOT| 设置此值后，相当于取反。除去此值的所有数据    |
| MATCHQUERY| 模糊匹配查询    |

    


  
