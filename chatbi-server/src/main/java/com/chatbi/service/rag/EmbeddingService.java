package com.chatbi.service.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量嵌入服务
 *
 * 为 NL2SQL RAG 提供语义检索能力：
 * - 将业务术语、表结构描述、历史查询转换为向量
 * - 根据用户问题检索最相似的上下文
 *
 * 当前使用内存存储（MVP），后续可替换为 Redis Vector / Milvus。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    // 内存存储：key -> (text, embedding_vector)
    private final Map<String, EmbeddingDocument> documentStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("EmbeddingService 初始化完成，当前文档数: {}", documentStore.size());
    }

    /**
     * 存储文档及其向量
     */
    public void store(String key, String text) {
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            documentStore.put(key, new EmbeddingDocument(text, embedding.vector()));
            log.debug("向量存储成功 - key: {}, text: {}", key, text.substring(0, Math.min(50, text.length())));
        } catch (Exception e) {
            log.warn("向量存储失败 - key: {}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 批量存储
     */
    public void storeAll(Map<String, String> documents) {
        documents.forEach(this::store);
    }

    /**
     * 语义检索：返回与 query 最相似的 topK 条文档
     */
    public List<SearchResult> search(String query, int topK) {
        if (documentStore.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            float[] queryVector = queryEmbedding.vector();

            return documentStore.entrySet().stream()
                .map(entry -> {
                    double similarity = cosineSimilarity(queryVector, entry.getValue().vector);
                    return new SearchResult(entry.getKey(), entry.getValue().text, similarity);
                })
                .sorted(Comparator.comparingDouble(SearchResult::similarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("向量检索失败 - query: {}, 原因: {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 清除所有文档
     */
    public void clear() {
        documentStore.clear();
        log.info("向量存储已清空");
    }

    /**
     * 删除指定 key 的文档
     */
    public void remove(String key) {
        documentStore.remove(key);
    }

    /**
     * 获取文档数量
     */
    public int size() {
        return documentStore.size();
    }

    /**
     * 获取所有文档 key
     */
    public Set<String> keys() {
        return new HashSet<>(documentStore.keySet());
    }

    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // 内部记录
    private record EmbeddingDocument(String text, float[] vector) {}

    // 检索结果
    public record SearchResult(String key, String text, double similarity) {}
}
