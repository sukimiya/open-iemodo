package com.iemodo.common.audit;

import com.iemodo.common.entity.BaseEntity;
import com.iemodo.common.util.SnowflakeIdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * BeforeConvertCallback 实现，用于在实体转换为数据库行之前：
 * <ul>
 *   <li>生成雪花算法ID（如果为空）</li>
 *   <li>设置默认值（status=1, isValid=true）</li>
 *   <li>标记是否为新增记录</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaseEntityBeforeConvertCallback implements BeforeConvertCallback<BaseEntity> {

    private final SnowflakeIdWorker idWorker;

    @Override
    public Publisher<BaseEntity> onBeforeConvert(BaseEntity entity, SqlIdentifier table) {
        // 1. 生成雪花算法ID（如果为空）
        if (entity.getId() == null) {
            long id = idWorker.nextId();
            entity.setId(id);
            entity.markNew();
            // 保持 isNew=true（默认值），让 BeforeSaveCallback 正确识别为新增
            log.debug("Generated snowflake ID: {} for table: {}", id, table);
        } else {
            // 如果ID不为空，说明是更新操作
            entity.markNotNew();
        }

        // 2. 设置默认值
        if (entity.getStatus() == null) {
            entity.setStatus(1); // 默认启用
        }
        if (entity.getIsValid() == null) {
            entity.setIsValid(true); // 默认有效
        }

        return Mono.just(entity);
    }
}
