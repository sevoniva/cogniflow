package com.chatbi.service;

import com.chatbi.entity.Synonym;
import com.chatbi.repository.SynonymMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 同义词服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SynonymService {

    private final SynonymMapper synonymMapper;

    /**
     * 查询所有同义词
     */
    public List<Synonym> list() {
        return synonymMapper.selectList(null);
    }

    /**
     * 根据 ID 查询同义词
     */
    public Synonym getById(Long id) {
        return synonymMapper.selectById(id);
    }

    /**
     * 创建同义词
     */
    @Transactional
    public Synonym create(Synonym synonym) {
        synonymMapper.insert(synonym);
        log.info("创建同义词成功：{} -> {}", synonym.getStandardWord(), synonym.getAliases());
        return synonym;
    }

    /**
     * 更新同义词
     */
    @Transactional
    public Synonym update(Long id, Synonym synonym) {
        Synonym existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("同义词不存在");
        }

        synonym.setId(id);
        synonymMapper.updateById(synonym);
        log.info("更新同义词成功：{}", synonym.getStandardWord());
        return synonym;
    }

    /**
     * 删除同义词
     */
    @Transactional
    public void delete(Long id) {
        synonymMapper.deleteById(id);
        log.info("删除同义词成功：{}", id);
    }
}
