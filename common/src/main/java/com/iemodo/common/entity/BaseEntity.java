package com.iemodo.common.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;

/**
 * 基础实体类，所有业务实体都应继承此类。
 * 
 * <p>提供统一的审计字段：
 * <ul>
 *   <li>id: 雪花算法生成的唯一ID</li>
 *   <li>status: 状态 (1-启用, 0-禁用)</li>
 *   <li>createBy: 创建人ID</li>
 *   <li>createTime: 创建时间 (UTC Instant)</li>
 *   <li>updateBy: 更新人ID</li>
 *   <li>updateTime: 更新时间 (UTC Instant)</li>
 *   <li>isValid: 软删除标志 (1-有效, 0-已删除)</li>
 * </ul>
 *
 * <p><strong>重要：</strong>所有子模块的实体类必须继承此类，数据库表必须使用以下字段名：
 * <pre>
 *   create_time  (timestamptz)
 *   update_time  (timestamptz)
 *   create_by    (bigint)
 *   update_by    (bigint)
 *   status       (int/integer)
 *   is_valid     (int/integer)
 * </pre>
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {

    /**
     * 主键ID，使用雪花算法生成
     */
    @Id
    @Column("id")
    private Long id;

    /**
     * 状态：1-启用，0-禁用
     */
    @Column("status")
    @Builder.Default
    private Integer status = 1;

    /**
     * 创建人ID
     */
    @Column("create_by")
    private Long createBy;

    /**
     * 创建时间 (UTC)
     */
    @Column("create_time")
    private Instant createTime;

    /**
     * 更新人ID
     */
    @Column("update_by")
    private Long updateBy;

    /**
     * 更新时间 (UTC)
     */
    @Column("update_time")
    private Instant updateTime;

    /**
     * 是否有效：1-有效，0-已删除（软删除）
     */
    @Column("is_valid")
    @Builder.Default
    private Boolean isValid = true;

    /**
     * 标记是否为新增操作（内部使用，不持久化）
     */
    @Transient
    @Builder.Default
    private transient boolean isNew = true;

    /**
     * 判断是否为新增记录
     */
    public boolean isNew() {
        return isNew || id == null;
    }

    /**
     * 标记为已存在的记录（更新操作时调用）
     */
    public void markNotNew() {
        this.isNew = false;
    }

    public void markNew() {
        this.isNew = true;
    }
}
