package com.iemodo.common.audit;

import com.iemodo.common.context.ReactiveUserContext;
import com.iemodo.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.BeforeSaveCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * BeforeSaveCallback 实现，用于在保存到数据库之前：
 * <ul>
 *   <li>设置创建时间/创建人（新增时）</li>
 *   <li>设置更新时间/更新人（始终）</li>
 *   <li>填充到 OutboundRow 供 R2DBC 使用</li>
 * </ul>
 * 
 * <p>使用 {@link Instant} (UTC) 作为时间戳类型，与时区无关，更科学
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaseEntityBeforeSaveCallback implements BeforeSaveCallback<BaseEntity> {

    @Override
    public Publisher<BaseEntity> onBeforeSave(BaseEntity entity, OutboundRow row, SqlIdentifier table) {
        Instant now = Instant.now();

        return ReactiveUserContext.getCurrentUserId()
                .defaultIfEmpty(0L) // 默认系统用户
                .flatMap(currentUserId -> {
                    // 判断是新增还是更新
                    boolean isNew = entity.isNew();

                    // 填充 ID（确保在 row 中）
                    if (entity.getId() != null) {
                        row.append("id", Parameter.from(entity.getId()));
                    }

                    if (isNew) {
                        // 新增操作：设置创建信息
                        if (entity.getCreateTime() == null) {
                            entity.setCreateTime(now);
                        }
                        if (entity.getCreateBy() == null) {
                            entity.setCreateBy(currentUserId);
                        }
                        row.append("create_time", Parameter.from(entity.getCreateTime()));
                        row.append("create_by", Parameter.from(entity.getCreateBy()));
                    }

                    // 无论新增还是更新，都设置更新时间
                    entity.setUpdateTime(now);
                    entity.setUpdateBy(currentUserId);
                    row.append("update_time", Parameter.from(now));
                    row.append("update_by", Parameter.from(currentUserId));

                    // 填充其他审计字段
                    if (entity.getStatus() != null) {
                        row.append("status", Parameter.from(entity.getStatus()));
                    }
                    if (entity.getIsValid() != null) {
                        row.append("is_valid", Parameter.from(entity.getIsValid()));
                    }

                    log.debug("Audit fields set for {} operation on table: {}, userId: {}, entityId: {}",
                            isNew ? "INSERT" : "UPDATE", table, currentUserId, entity.getId());

                    return Mono.just(entity);
                });
    }
}
