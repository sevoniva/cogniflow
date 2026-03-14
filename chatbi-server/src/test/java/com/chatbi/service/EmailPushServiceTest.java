package com.chatbi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailPushServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void shouldSendMailWhenProviderAvailable() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        EmailPushService service = new EmailPushService(mailSenderProvider);

        service.push("ops@chatbi.com", "日报", "今日指标更新完成");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("日报", message.getSubject());
        assertEquals("今日指标更新完成", message.getText());
        assertEquals("ops@chatbi.com", message.getTo()[0]);
    }

    @Test
    void shouldFailWhenMailSenderMissing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        EmailPushService service = new EmailPushService(mailSenderProvider);

        assertThrows(IllegalStateException.class, () -> service.push("ops@chatbi.com", "日报", "内容"));
    }
}
