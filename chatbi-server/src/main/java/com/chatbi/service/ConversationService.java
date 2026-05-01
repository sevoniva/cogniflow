package com.chatbi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 对话上下文管理服务
 * 实现多轮对话能力
 */
@Slf4j
@Service
public class ConversationService {

    private final ObjectMapper objectMapper;
    private final Map<String, Conversation> localConversationStore = new ConcurrentHashMap<>();

    private StringRedisTemplate redisTemplate;
    private volatile boolean redisFallbackLogged;
    @Value("${app.redis.enabled:false}")
    private boolean conversationRedisEnabled = false;

    private static final String CONVERSATION_PREFIX = "chatbi:conversation:";
    private static final long CONVERSATION_TIMEOUT = 30; // 30分钟过期

    @Autowired
    public ConversationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ConversationService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Autowired(required = false)
    void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 对话消息
     */
    public static class Message {
        private String role; // user / assistant
        private String content;
        private String sql;
        private Map<String, Object> metadata;
        private long timestamp;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 对话上下文
     */
    public static class Conversation {
        private String conversationId;
        private Long userId;
        private List<Message> messages;
        private Map<String, Object> context; // 存储上下文信息（如：当前表、当前时间范围等）
        private long createdAt;
        private long updatedAt;

        public Conversation() {
            this.messages = new ArrayList<>();
            this.context = new HashMap<>();
            this.createdAt = System.currentTimeMillis();
            this.updatedAt = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * 创建新对话
     */
    public Conversation createConversation(Long userId) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setUserId(userId);

        saveConversation(conversation);
        log.info("创建新对话 - conversationId: {}, userId: {}", conversation.getConversationId(), userId);

        return conversation;
    }

    /**
     * 获取对话
     */
    public Conversation getConversation(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;

        try {
            if (conversationRedisEnabled && redisTemplate != null) {
                String json = redisTemplate.opsForValue().get(key);

                if (json == null) {
                    return getConversationFromLocal(conversationId);
                }

                Conversation conversation = objectMapper.readValue(json, Conversation.class);
                log.info("获取对话 - conversationId: {}, 消息数: {}", conversationId, conversation.getMessages().size());
                return conversation;
            }
        } catch (Exception e) {
            logRedisFallback("读取", conversationId, e);
        }

        return getConversationFromLocal(conversationId);
    }

    /**
     * 保存对话
     */
    public void saveConversation(Conversation conversation) {
        String key = CONVERSATION_PREFIX + conversation.getConversationId();
        try {
            conversation.setUpdatedAt(System.currentTimeMillis());
            localConversationStore.put(key, deepCopy(conversation));

            if (conversationRedisEnabled && redisTemplate != null) {
                String json = objectMapper.writeValueAsString(conversation);
                redisTemplate.opsForValue().set(key, json, CONVERSATION_TIMEOUT, TimeUnit.MINUTES);
            }
            log.info("保存对话 - conversationId: {}, 消息数: {}", conversation.getConversationId(), conversation.getMessages().size());
        } catch (Exception e) {
            localConversationStore.put(key, deepCopy(conversation));
            logRedisFallback("写入", conversation.getConversationId(), e);
        }
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String conversationId, String content) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            log.warn("对话不存在，无法添加消息 - conversationId: {}", conversationId);
            return;
        }

        Message message = new Message("user", content);
        conversation.getMessages().add(message);

        saveConversation(conversation);
        log.info("添加用户消息 - conversationId: {}, content: {}", conversationId, content);
    }

    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String conversationId, String content, String sql, Map<String, Object> metadata) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            log.warn("对话不存在，无法添加消息 - conversationId: {}", conversationId);
            return;
        }

        Message message = new Message("assistant", content);
        message.setSql(sql);
        message.setMetadata(metadata);

        conversation.getMessages().add(message);

        saveConversation(conversation);
        log.info("添加助手消息 - conversationId: {}, content: {}", conversationId, content);
    }

    /**
     * 更新对话上下文
     */
    public void updateContext(String conversationId, String key, Object value) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            log.warn("对话不存在，无法更新上下文 - conversationId: {}", conversationId);
            return;
        }

        conversation.getContext().put(key, value);
        saveConversation(conversation);

        log.info("更新对话上下文 - conversationId: {}, key: {}, value: {}", conversationId, key, value);
    }

    /**
     * 获取对话历史（用于LLM上下文）
     */
    @Observed(name = "conversation.history", contextualName = "get-recent-messages")
    public List<Message> getRecentMessages(String conversationId, int limit) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null) {
            return new ArrayList<>();
        }

        List<Message> messages = conversation.getMessages();
        int start = Math.max(0, messages.size() - limit);

        return messages.subList(start, messages.size());
    }

    /**
     * 构建LLM上下文提示词
     */
    public String buildContextPrompt(String conversationId) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null || conversation.getMessages().isEmpty()) {
            return "";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("## 对话历史\n\n");

        // 获取最近5轮对话
        List<Message> recentMessages = getRecentMessages(conversationId, 10);

        for (Message message : recentMessages) {
            if ("user".equals(message.getRole())) {
                prompt.append("用户：").append(message.getContent()).append("\n");
            } else {
                prompt.append("助手：").append(message.getContent()).append("\n");
                if (message.getSql() != null) {
                    prompt.append("SQL：").append(message.getSql()).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 添加上下文信��
        Map<String, Object> context = conversation.getContext();
        if (!context.isEmpty()) {
            prompt.append("## 当前上下文\n\n");
            context.forEach((key, value) -> {
                prompt.append("- ").append(key).append(": ").append(value).append("\n");
            });
            prompt.append("\n");
        }

        return prompt.toString();
    }

    /**
     * 分析用户意图（是否是追问）
     */
    public boolean isFollowUpQuestion(String conversationId, String question) {
        Conversation conversation = getConversation(conversationId);
        if (conversation == null || conversation.getMessages().isEmpty()) {
            return false;
        }
        if (conversation.getMessages().size() < 2) {
            return false;
        }

        // 简单的追问判断规则
        String lowerQuestion = question.toLowerCase();

        // 包含指代词
        if (lowerQuestion.contains("它") || lowerQuestion.contains("这个") || lowerQuestion.contains("那个") ||
            lowerQuestion.contains("呢") || lowerQuestion.contains("对比") || lowerQuestion.contains("比较")) {
            return true;
        }

        // 包含时间对比词
        if (lowerQuestion.contains("上") || lowerQuestion.contains("去年") || lowerQuestion.contains("同比") ||
            lowerQuestion.contains("环比") || lowerQuestion.contains("增长")) {
            return true;
        }

        return false;
    }

    /**
     * 删除对话
     */
    public void deleteConversation(String conversationId) {
        String key = CONVERSATION_PREFIX + conversationId;
        localConversationStore.remove(key);
        if (conversationRedisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                logRedisFallback("删除", conversationId, e);
            }
        }
        log.info("删除对话 - conversationId: {}", conversationId);
    }

    /**
     * 获取用户的所有对话
     */
    public List<Conversation> getUserConversations(Long userId) {
        // 这里简化实现，实际应该维护用户对话列表
        // 可以使用 Redis Set 存储用户的对话ID列表
        return localConversationStore.values().stream()
            .filter(conversation -> Objects.equals(conversation.getUserId(), userId))
            .sorted(Comparator.comparingLong(Conversation::getUpdatedAt).reversed())
            .map(this::deepCopy)
            .toList();
    }

    private Conversation getConversationFromLocal(String conversationId) {
        Conversation conversation = localConversationStore.get(CONVERSATION_PREFIX + conversationId);
        if (conversation == null) {
            log.warn("对话不存在 - conversationId: {}", conversationId);
            return null;
        }
        return deepCopy(conversation);
    }

    private Conversation deepCopy(Conversation conversation) {
        try {
            String json = objectMapper.writeValueAsString(conversation);
            return objectMapper.readValue(json, Conversation.class);
        } catch (Exception e) {
            throw new IllegalStateException("对话对象复制失败", e);
        }
    }

    private void logRedisFallback(String action, String conversationId, Exception e) {
        if (!redisFallbackLogged) {
            redisFallbackLogged = true;
            log.warn("Redis 对话{}失败，已降级为本地存储 - conversationId: {}", action, conversationId, e);
            return;
        }
        log.debug("Redis 对话{}失败，继续使用本地存储 - conversationId: {}", action, conversationId, e);
    }
}
