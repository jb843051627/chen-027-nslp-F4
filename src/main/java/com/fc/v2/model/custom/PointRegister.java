package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 点位建档上报行（建档岗一条一条建／复测核收带出，都装进这一个形状）。
 *
 * 屏上看到的那一行与库里的那一行必须是同一行：id 指着库里那一行自己时按更新办，
 * id 空着是建行。还差几回不在这张形状上，由核定应测、已达标两栏轧出来。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "PointRegister", description = "点位建档上报行")
public class PointRegister implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 库里那一行自己的主键；空着=建行，指着自己=更新，不许被同码挡在门外 */
    @ApiModelProperty(value = "行主键（空为建行）")
    private Long id;

    /** 点位码（随报送材料进来，不另发、不核构成规矩） */
    @ApiModelProperty(value = "点位码")
    private String pointNo;

    /** 年度（同一年度同一区册底下去重） */
    @ApiModelProperty(value = "年度")
    private Integer annual;

    /** 区册代号（行上一栏，拿到区册底里去找；找不到错落在这一栏） */
    @ApiModelProperty(value = "区册代号")
    private String zoneNo;

    /** 区册全称（行上冗余的一栏，对不齐以区册为准改齐） */
    @ApiModelProperty(value = "区册全称")
    private String zoneName;

    /** 核定应测次数（收报文字符串，是不是整数由口径处一句句点名） */
    @ApiModelProperty(value = "核定应测次数")
    private String needNum;

    /** 已核验达标的次数（空着按 0 认） */
    @ApiModelProperty(value = "已核验达标的次数")
    private String gotNum;

    /** 随件交来的点位题名与依附声源（出文取值项之一） */
    @ApiModelProperty(value = "点位题名与依附声源")
    private String content;

    /** 来路：建档岗建／复测核收带出 */
    @ApiModelProperty(value = "来路")
    private String srcKind;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public Integer getAnnual() {
        return annual;
    }

    public void setAnnual(Integer annual) {
        this.annual = annual;
    }

    public String getZoneNo() {
        return zoneNo;
    }

    public void setZoneNo(String zoneNo) {
        this.zoneNo = zoneNo;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getNeedNum() {
        return needNum;
    }

    public void setNeedNum(String needNum) {
        this.needNum = needNum;
    }

    public String getGotNum() {
        return gotNum;
    }

    public void setGotNum(String gotNum) {
        this.gotNum = gotNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSrcKind() {
        return srcKind;
    }

    public void setSrcKind(String srcKind) {
        this.srcKind = srcKind;
    }
}
