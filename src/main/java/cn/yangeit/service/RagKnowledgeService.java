package cn.yangeit.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusSearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RagKnowledgeService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeService.class);
    private static final String EMPTY_CONTEXT = "No knowledge base context matched this query.";

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Value("${app.rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${app.rag.vector-enabled:false}")
    private boolean vectorEnabled;

    @Value("${app.rag.vector-provider:simple}")
    private String vectorProvider;

    @Value("${app.rag.top-k:3}")
    private int topK;

    @Value("${app.rag.keyword-limit:3}")
    private int keywordLimit;

    @Value("${app.rag.embedding-dimension:1536}")
    private int embeddingDimension;

    @Value("${app.rag.milvus.uri:}")
    private String milvusUri;

    @Value("${app.rag.milvus.host:localhost}")
    private String milvusHost;

    @Value("${app.rag.milvus.port:19530}")
    private int milvusPort;

    @Value("${app.rag.milvus.token:}")
    private String milvusToken;

    @Value("${app.rag.milvus.database-name:default}")
    private String milvusDatabaseName;

    @Value("${app.rag.milvus.collection-name:fresh_customer_service_knowledge}")
    private String milvusCollectionName;

    @Value("${app.rag.milvus.index-type:AUTOINDEX}")
    private String milvusIndexType;

    @Value("${app.rag.milvus.metric-type:COSINE}")
    private String milvusMetricType;

    @Value("${app.rag.milvus.index-parameters:}")
    private String milvusIndexParameters;

    @Value("${app.rag.milvus.initialize-schema:true}")
    private boolean milvusInitializeSchema;

    @Value("${app.rag.milvus.search-params-json:}")
    private String milvusSearchParamsJson;

    private volatile boolean vectorReady;
    private volatile VectorStore vectorStore;
    private volatile MilvusServiceClient milvusClient;
    private List<Document> knowledgeChunks = List.of();

    public RagKnowledgeService(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
    }

    @PostConstruct
    public void init() {
        if (!ragEnabled) {
            log.info("RAG is disabled by configuration.");
            return;
        }

        this.knowledgeChunks = loadKnowledgeChunks();
        if (knowledgeChunks.isEmpty()) {
            log.warn("No RAG documents were loaded from classpath:rag/*.txt");
            return;
        }

        if (!vectorEnabled) {
            log.info("RAG keyword retrieval is enabled. Vector retrieval is disabled.");
            return;
        }

        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            log.warn("No EmbeddingModel bean is available. Falling back to keyword retrieval.");
            return;
        }

        try {
            if ("milvus".equalsIgnoreCase(vectorProvider)) {
                initializeMilvusVectorStore(embeddingModel);
            } else {
                initializeSimpleVectorStore(embeddingModel);
            }
        } catch (Exception e) {
            this.vectorReady = false;
            this.vectorStore = null;
            log.warn("Failed to initialize vector retrieval provider [{}]. Falling back to keyword retrieval.",
                    vectorProvider, e);
        }
    }

    public String buildContext(String query) {
        List<Document> documents = retrieve(query);
        if (documents.isEmpty()) {
            return EMPTY_CONTEXT;
        }

        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (Document document : documents) {
            String source = String.valueOf(document.getMetadata().getOrDefault("source", "knowledge-base"));
            String content = normalizeWhitespace(document.getText());
            if (content.length() > 220) {
                content = content.substring(0, 220) + "...";
            }
            builder.append("Snippet ")
                    .append(index++)
                    .append(" [source=")
                    .append(source)
                    .append("]: ")
                    .append(content)
                    .append('\n');
        }
        return builder.toString().trim();
    }

    public boolean isVectorReady() {
        return vectorReady;
    }

    public String getVectorProvider() {
        return vectorReady ? vectorProvider : "keyword";
    }

    @Override
    public void destroy() throws Exception {
        if (milvusClient != null) {
            milvusClient.close(5_000L);
        }
    }

    private void initializeSimpleVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(knowledgeChunks);
        this.vectorStore = store;
        this.vectorReady = true;
        log.info("RAG simple vector store initialized with {} chunks.", knowledgeChunks.size());
    }

    private void initializeMilvusVectorStore(EmbeddingModel embeddingModel) throws Exception {
        this.milvusClient = new MilvusServiceClient(buildConnectParam());

        MilvusVectorStore.Builder builder = MilvusVectorStore.builder(milvusClient, embeddingModel)
                .databaseName(milvusDatabaseName)
                .collectionName(milvusCollectionName)
                .embeddingDimension(embeddingDimension)
                .metricType(resolveMetricType(milvusMetricType))
                .indexType(resolveIndexType(milvusIndexType))
                .initializeSchema(milvusInitializeSchema);

        if (!milvusIndexParameters.isBlank()) {
            builder.indexParameters(milvusIndexParameters);
        }

        MilvusVectorStore store = builder.build();
        store.afterPropertiesSet();
        store.add(knowledgeChunks);

        this.vectorStore = store;
        this.vectorReady = true;
        log.info("RAG Milvus vector store initialized. collection={}, dimension={}",
                milvusCollectionName, embeddingDimension);
    }

    private ConnectParam buildConnectParam() {
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withConnectTimeout(10, TimeUnit.SECONDS)
                .withKeepAliveTime(30, TimeUnit.SECONDS)
                .withKeepAliveTimeout(10, TimeUnit.SECONDS);

        if (!milvusUri.isBlank()) {
            builder.withUri(milvusUri);
        } else {
            builder.withHost(milvusHost).withPort(milvusPort);
        }

        if (!milvusToken.isBlank()) {
            builder.withToken(milvusToken);
        }

        if (!milvusDatabaseName.isBlank()) {
            builder.withDatabaseName(milvusDatabaseName);
        }

        return builder.build();
    }

    private List<Document> retrieve(String query) {
        if (!ragEnabled || query == null || query.isBlank() || knowledgeChunks.isEmpty()) {
            return List.of();
        }

        if (vectorReady && vectorStore != null) {
            try {
                List<Document> documents = vectorStore.similaritySearch(buildSearchRequest(query));
                if (documents != null && !documents.isEmpty()) {
                    return documents;
                }
            } catch (Exception e) {
                log.warn("Vector retrieval failed for query [{}]. Falling back to keyword retrieval.", query, e);
            }
        }

        return keywordSearch(query);
    }

    private SearchRequest buildSearchRequest(String query) {
        if ("milvus".equalsIgnoreCase(vectorProvider)) {
            MilvusSearchRequest.MilvusBuilder builder = MilvusSearchRequest.milvusBuilder()
                    .query(query)
                    .topK(topK)
                    .similarityThresholdAll();
            if (!milvusSearchParamsJson.isBlank()) {
                builder.searchParamsJson(milvusSearchParamsJson);
            }
            return builder.build();
        }

        return SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThresholdAll()
                .build();
    }

    private List<Document> keywordSearch(String query) {
        String normalizedQuery = normalizeSearchText(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        List<String> terms = buildTerms(normalizedQuery);
        return knowledgeChunks.stream()
                .map(document -> Map.entry(document, score(document.getText(), normalizedQuery, terms)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.<Map.Entry<Document, Integer>, Integer>comparing(Map.Entry::getValue).reversed())
                .limit(keywordLimit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int score(String content, String normalizedQuery, List<String> terms) {
        String normalizedContent = normalizeSearchText(content);
        int score = 0;

        if (normalizedContent.contains(normalizedQuery)) {
            score += 10;
        }

        for (String term : terms) {
            if (!term.isBlank() && normalizedContent.contains(term)) {
                score += Math.min(term.length(), 4);
            }
        }
        return score;
    }

    private List<Document> loadKnowledgeChunks() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:rag/*.txt");
            if (resources.length == 0) {
                return List.of();
            }

            List<Document> allDocuments = new ArrayList<>();
            TokenTextSplitter splitter = new TokenTextSplitter();
            for (Resource resource : resources) {
                List<Document> documents = new TextReader(resource).get();
                for (Document document : documents) {
                    document.getMetadata().put("source", resource.getFilename());
                }
                allDocuments.addAll(splitter.apply(documents));
            }
            return Collections.unmodifiableList(allDocuments);
        } catch (Exception e) {
            log.error("Failed to load RAG documents.", e);
            return List.of();
        }
    }

    private List<String> buildTerms(String normalizedQuery) {
        Set<String> terms = new HashSet<>();
        terms.add(normalizedQuery);

        for (String token : normalizedQuery.split("\\s+")) {
            if (!token.isBlank()) {
                terms.add(token);
                if (token.length() > 2) {
                    for (int i = 0; i < token.length() - 1; i++) {
                        terms.add(token.substring(i, i + 2));
                    }
                }
            }
        }
        return new ArrayList<>(terms);
    }

    private MetricType resolveMetricType(String value) {
        try {
            return MetricType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return MetricType.COSINE;
        }
    }

    private IndexType resolveIndexType(String value) {
        try {
            return IndexType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return IndexType.AUTOINDEX;
        }
    }

    private String normalizeWhitespace(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}，。！？；：、“”‘’（）【】《》\\s]+", " ")
                .trim();
    }
}
