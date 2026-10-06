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
 * 执法文书出文档案对象 t_nslp_doc_issue
 *
 * 一纸一版：出文那一刻照的是哪一版，就把版次与三项数钉在档上；
 * 往后新立一版只供新文，绝不回改旧档。
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_nslp_doc_issue")
@ApiModel(value = "TNslpDocIssue", description = "执法文书出文档案")
public class TNslpDocIssue implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 照哪一行点位出的文 */
    @TableField("point_id")
    @ApiModelProperty(value = "照哪一行点位出的文")
    private Long pointId;

    /** 出文时点位码（冗余快照） */
    @TableField("point_no")
    @ApiModelProperty(value = "出文时点位码")
    private String pointNo;

    /** 出文那一刻照的那一版主键 */
    @TableField("tpl_id")
    @ApiModelProperty(value = "出文那一刻照的版主键")
    private Long tplId;

    /** 文书模板代号（快照） */
    @TableField("tpl_no")
    @ApiModelProperty(value = "文书模板代号")
    private String tplNo;

    /** 文种名（快照） */
    @TableField("tpl_name")
    @ApiModelProperty(value = "文种名")
    private String tplName;

    /** 版次（出文那一刻的开口版，钉死） */
    @TableField("ver_no")
    @ApiModelProperty(value = "版次")
    private Integer verNo;

    /** 本版应填项数（快照） */
    @TableField("var_num")
    @ApiModelProperty(value = "本版应填项数")
    private Integer varNum;

    /** 已取到值的项数（快照） */
    @TableField("fill_num")
    @ApiModelProperty(value = "已取到值的项数")
    private Integer fillNum;

    /** 还差几项（快照） */
    @TableField("miss_num")
    @ApiModelProperty(value = "还差几项")
    private Integer missNum;

    /** 出文那一刻 */
    @TableField("issued_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "出文那一刻")
    private Date issuedAt;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者（谁出的文） */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

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

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public Long getTplId() {
        return tplId;
    }

    public void setTplId(Long tplId) {
        this.tplId = tplId;
    }

    public String getTplNo() {
        return tplNo;
    }

    public void setTplNo(String tplNo) {
        this.tplNo = tplNo;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public Integer getVerNo() {
        return verNo;
    }

    public void setVerNo(Integer verNo) {
        this.verNo = verNo;
    }

    public Integer getVarNum() {
        return varNum;
    }

    public void setVarNum(Integer varNum) {
        this.varNum = varNum;
    }

    public Integer getFillNum() {
        return fillNum;
    }

    public void setFillNum(Integer fillNum) {
        this.fillNum = fillNum;
    }

    public Integer getMissNum() {
        return missNum;
    }

    public void setMissNum(Integer missNum) {
        this.missNum = missNum;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
