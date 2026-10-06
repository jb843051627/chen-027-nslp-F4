package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 点位在走留痕对象 t_nslp_point_trail
 *
 * 待测→已测→压卡每一步按时序压在点位行身后；收回重来时先前那一趟照旧留着。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_nslp_point_trail")
@ApiModel(value = "TNslpPointTrail", description = "点位在走留痕")
public class TNslpPointTrail implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所随点位 t_nslp_point.id */
    @TableField("point_id")
    @ApiModelProperty(value = "所随点位")
    private Long pointId;

    /** 落笔那一刻所处步数 0待测 1已测 2压卡 */
    @TableField("step")
    @ApiModelProperty(value = "落笔那一刻所处步数")
    private Integer step;

    /** 动作 建行/推进/收回/叫停/压卡 */
    @TableField("action")
    @ApiModelProperty(value = "动作")
    private String action;

    /** 提交那一刻 */
    @TableField("trail_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "提交那一刻")
    private Date trailTime;

    /** 同刻先后次序 */
    @TableField("seq")
    @ApiModelProperty(value = "同刻先后次序")
    private Integer seq;

    /** 这一笔留下的话 */
    @TableField("note")
    @ApiModelProperty(value = "这一笔留下的话")
    private String note;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPointId() {
        return pointId;
    }

    public void setPointId(Long pointId) {
        this.pointId = pointId;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Date getTrailTime() {
        return trailTime;
    }

    public void setTrailTime(Date trailTime) {
        this.trailTime = trailTime;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
