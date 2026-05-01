package com.chatbi.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chatbi.entity.PromptVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 版本 Mapper
 */
@Mapper
public interface PromptVersionMapper extends BaseMapper<PromptVersion> {

    /**
     * 查询生效中的 Prompt 版本
     */
    @Select("SELECT * FROM prompt_version WHERE status = 'active' AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT 1")
    Optional<PromptVersion> findLatestActive();

    /**
     * 查询所有非废弃的 Prompt 版本
     */
    @Select("SELECT * FROM prompt_version WHERE status != 'deprecated' AND deleted_at IS NULL ORDER BY updated_at DESC")
    List<PromptVersion> findAllActiveOrDraft();

    /**
     * 将其他 active 版本置为 deprecated
     */
    @Update("UPDATE prompt_version SET status = 'deprecated', updated_at = CURRENT_TIMESTAMP WHERE status = 'active' AND id != #{id} AND deleted_at IS NULL")
    int deprecateOthers(@Param("id") Long id);
}
