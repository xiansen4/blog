package com.xians.yaco.repository;


import com.xians.yaco.model.domain.Tag;
import com.xians.yaco.repository.base.BaseRepository;

/**
 * <pre>
 *     标签持久层
 * </pre>
 *
 * @author : XIANS
 * @date : 2020/4/15
 */
public interface TagRepository extends BaseRepository<Tag, Long> {

    /**
     * 根据标签路径查询，用于验证是否已经存在该路径
     *
     * @param tagUrl tagUrl
     * @return Tag
     */
    Tag findTagByTagUrl(String tagUrl);

    /**
     * 根据标签名称查询
     *
     * @param tagName 标签名
     * @return Tag
     */
    Tag findTagByTagName(String tagName);
}
