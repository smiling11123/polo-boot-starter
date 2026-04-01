package com.polo.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.polo.boot.mybatis.plus.annotation.AutoFillField;
import com.polo.boot.mybatis.plus.annotation.AutoFillType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("demo_record")
public class DemoRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String status;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.TENANT_ID)
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.DEPT_ID)
    private Long deptId;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.CREATE_BY)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @AutoFillField(type = AutoFillType.UPDATE_BY)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT)
    @AutoFillField(type = AutoFillType.CREATE_TIME)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @AutoFillField(type = AutoFillType.UPDATE_TIME)
    private LocalDateTime updateTime;
}
