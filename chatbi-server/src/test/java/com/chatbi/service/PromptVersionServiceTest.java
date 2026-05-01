package com.chatbi.service;

import com.chatbi.entity.PromptVersion;
import com.chatbi.repository.PromptVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prompt 版本管理服务测试
 */
@ExtendWith(MockitoExtension.class)
class PromptVersionServiceTest {

    @Mock
    private PromptVersionMapper promptVersionMapper;

    @InjectMocks
    private PromptVersionService promptVersionService;

    @Test
    void create_shouldSetDefaultsAndInsert() {
        PromptVersion input = PromptVersion.builder()
                .name("测试版本")
                .versionTag("test-v1")
                .template("{{question}}")
                .build();

        when(promptVersionMapper.insert(any())).thenAnswer(inv -> {
            PromptVersion v = inv.getArgument(0);
            v.setId(1L);
            return 1;
        });

        PromptVersion result = promptVersionService.create(input);

        assertNotNull(result.getId());
        assertEquals("draft", result.getStatus());
        assertEquals(0, result.getGrayScalePercent());
    }

    @Test
    void resolveForUser_shouldReturnGrayVersion_whenUserInGrayGroup() {
        PromptVersion gray = PromptVersion.builder()
                .id(2L).name("灰度版").versionTag("gray-v1")
                .status("active").grayScalePercent(10).template("gray").build();
        PromptVersion stable = PromptVersion.builder()
                .id(1L).name("稳定版").versionTag("stable-v1")
                .status("active").grayScalePercent(0).template("stable").build();

        when(promptVersionMapper.findAllActiveOrDraft()).thenReturn(List.of(gray, stable));

        // userId=10 -> 10 % 100 = 10, 10 < 10 is false, so stable
        Optional<PromptVersion> v10 = promptVersionService.resolveForUser(10L);
        assertTrue(v10.isPresent());
        assertEquals("stable-v1", v10.get().getVersionTag());

        // userId=5 -> 5 % 100 = 5, 5 < 10 is true, so gray
        Optional<PromptVersion> v5 = promptVersionService.resolveForUser(5L);
        assertTrue(v5.isPresent());
        assertEquals("gray-v1", v5.get().getVersionTag());
    }

    @Test
    void resolveForUser_shouldReturnStable_whenNoGrayVersion() {
        PromptVersion stable = PromptVersion.builder()
                .id(1L).name("稳定版").versionTag("stable-v1")
                .status("active").grayScalePercent(0).template("stable").build();

        when(promptVersionMapper.findAllActiveOrDraft()).thenReturn(List.of(stable));

        Optional<PromptVersion> result = promptVersionService.resolveForUser(100L);
        assertTrue(result.isPresent());
        assertEquals("stable-v1", result.get().getVersionTag());
    }

    @Test
    void resolveForUser_shouldReturnEmpty_whenNoVersions() {
        when(promptVersionMapper.findAllActiveOrDraft()).thenReturn(List.of());

        Optional<PromptVersion> result = promptVersionService.resolveForUser(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void activate_shouldDeprecateOthers() {
        PromptVersion version = PromptVersion.builder()
                .id(1L).name("新版").versionTag("v2").status("draft").template("t").build();

        when(promptVersionMapper.selectById(1L)).thenReturn(version);
        when(promptVersionMapper.deprecateOthers(1L)).thenReturn(1);

        PromptVersion result = promptVersionService.activate(1L);

        assertEquals("active", result.getStatus());
        verify(promptVersionMapper).deprecateOthers(1L);
    }

    @Test
    void duplicate_shouldCreateCopy() {
        PromptVersion source = PromptVersion.builder()
                .id(1L).name("原版").versionTag("v1")
                .template("template").variables("{}").description("desc").build();

        when(promptVersionMapper.selectById(1L)).thenReturn(source);
        when(promptVersionMapper.insert(any())).thenAnswer(inv -> {
            PromptVersion v = inv.getArgument(0);
            v.setId(2L);
            return 1;
        });

        PromptVersion copy = promptVersionService.duplicate(1L);

        assertEquals(2L, copy.getId());
        assertEquals("draft", copy.getStatus());
        assertTrue(copy.getName().contains("副本"));
        assertTrue(copy.getVersionTag().startsWith("v1-copy-"));
    }
}
