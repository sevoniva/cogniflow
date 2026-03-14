package com.chatbi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对话服务测试
 */
class ConversationServiceTest {

    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(new ObjectMapper());
    }

    @Test
    void testCreateConversation() {
        ConversationService.Conversation conversation = conversationService.createConversation(1L);

        assertNotNull(conversation);
        assertNotNull(conversation.getConversationId());
        assertEquals(1L, conversation.getUserId());
        assertTrue(conversation.getMessages().isEmpty());
        assertTrue(conversation.getContext().isEmpty());
        assertTrue(conversation.getCreatedAt() > 0);
    }

    @Test
    void testAddUserMessage() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");
        conversation.setUserId(1L);
        conversationService.saveConversation(conversation);

        conversationService.addUserMessage("test-id", "测试消息");

        ConversationService.Conversation saved = conversationService.getConversation("test-id");
        assertNotNull(saved);
        assertEquals(1, saved.getMessages().size());
        assertEquals("user", saved.getMessages().get(0).getRole());
        assertEquals("测试消息", saved.getMessages().get(0).getContent());
    }

    @Test
    void testAddAssistantMessage() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");
        conversation.setUserId(1L);
        conversationService.saveConversation(conversation);

        conversationService.addAssistantMessage("test-id", "回答", "SELECT * FROM test", java.util.Map.of("dataCount", 10));

        ConversationService.Conversation saved = conversationService.getConversation("test-id");
        assertNotNull(saved);
        assertEquals(1, saved.getMessages().size());
        assertEquals("assistant", saved.getMessages().get(0).getRole());
        assertEquals("回答", saved.getMessages().get(0).getContent());
        assertEquals("SELECT * FROM test", saved.getMessages().get(0).getSql());
    }

    @Test
    void testIsFollowUpQuestion_True() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");
        conversation.getMessages().add(new ConversationService.Message("user", "本月销售额"));
        conversation.getMessages().add(new ConversationService.Message("assistant", "本月销售额为 120 万"));
        conversationService.saveConversation(conversation);

        boolean isFollowUp = conversationService.isFollowUpQuestion("test-id", "那上月呢？");

        assertTrue(isFollowUp, "包含'呢'应该被识别为追问");
    }

    @Test
    void testIsFollowUpQuestion_False() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");
        conversation.getMessages().add(new ConversationService.Message("user", "本月销售额"));
        conversationService.saveConversation(conversation);

        boolean isFollowUp = conversationService.isFollowUpQuestion("test-id", "本月销售额是多少？");

        assertFalse(isFollowUp, "独立问题不应被识别为追问");
    }

    @Test
    void testBuildContextPrompt() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");

        ConversationService.Message msg1 = new ConversationService.Message("user", "本月销售额");
        ConversationService.Message msg2 = new ConversationService.Message("assistant", "100万");
        msg2.setSql("SELECT SUM(amount) FROM sales");

        conversation.getMessages().add(msg1);
        conversation.getMessages().add(msg2);
        conversation.getContext().put("lastQuery", "本月销售额");
        conversationService.saveConversation(conversation);

        String prompt = conversationService.buildContextPrompt("test-id");

        assertNotNull(prompt);
        assertTrue(prompt.contains("对话历史"), "应该包含对话历史标题");
        assertTrue(prompt.contains("本月销售额"), "应该包含用户消息");
        assertTrue(prompt.contains("100万"), "应该包含助手消息");
        assertTrue(prompt.contains("当前上下文"), "应该包含上下文信息");
    }

    @Test
    void testUpdateContext() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");
        conversationService.saveConversation(conversation);

        conversationService.updateContext("test-id", "testKey", "testValue");

        ConversationService.Conversation saved = conversationService.getConversation("test-id");
        assertNotNull(saved);
        assertEquals("testValue", saved.getContext().get("testKey"));
    }

    @Test
    void testDeleteConversation() {
        ConversationService.Conversation conversation = new ConversationService.Conversation();
        conversation.setConversationId("test-id");
        conversationService.saveConversation(conversation);

        conversationService.deleteConversation("test-id");

        assertNull(conversationService.getConversation("test-id"));
    }
}
