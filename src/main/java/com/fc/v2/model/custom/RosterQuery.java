package com.fc.v2.model.custom;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 名册摆法问话：分页、三处搜索、可见范围都装进这一个形状。
 *
 * 三处搜索（点位码、区册代号、区层类别）前缀匹配与全等匹配都认；
 * 可见范围按区层切：本层值守岗只看见挂靠本层的行，区局岗四层都看得见
 * 但仍限划归它的那些区册。三种视图（按区层类别／按区册代号／按入册情形）
 * 同一年度同一批行处数必须一样——底数只取一份。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "RosterQuery", description = "名册摆法问话")
public class RosterQuery implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 年度（必填；本年度一行没建时视图留空是合法结果） */
    @ApiModelProperty(value = "年度")
    private Integer annual;

    /** 搜索：点位码（前缀/全等都认，空着不限） */
    @ApiModelProperty(value = "点位码搜索")
    private String pointNo;

    /** 搜索：区册代号（前缀/全等都认） */
    @ApiModelProperty(value = "区册代号搜索")
    private String zoneNo;

    /** 搜索：区层类别（前缀/全等都认：甲乙丙丁层） */
    @ApiModelProperty(value = "区层类别搜索")
    private String zoneKind;

    /** 全等才认：true 只认一字不差；false（默认）前缀与全等都认 */
    @ApiModelProperty(value = "是否只认全等")
    private Boolean exact;

    /** 本层值守岗只看得见这一层；区局岗留空（四层都看得见） */
    @ApiModelProperty(value = "值守岗本层类别")
    private String viewerLayer;

    /** 区局岗看得见的区册代号范围（划归它的那些册；空着=一册都不给） */
    @ApiModelProperty(value = "区局岗可见区册范围")
    private List<String> viewerZoneNos;

    /** 视图：byKind 按区层类别／byZone 按区册代号／byIntake 按入册情形 */
    @ApiModelProperty(value = "视图摆法")
    private String view;

    /** 页码（从 1 起；每页几条由区局配置，不在问话里） */
    @ApiModelProperty(value = "页码")
    private Integer pageNum;

    public Integer getAnnual() {
        return annual;
    }

    public void setAnnual(Integer annual) {
        this.annual = annual;
    }

    public String getPointNo() {
        return pointNo;
    }

    public void setPointNo(String pointNo) {
        this.pointNo = pointNo;
    }

    public String getZoneNo() {
        return zoneNo;
    }

    public void setZoneNo(String zoneNo) {
        this.zoneNo = zoneNo;
    }

    public String getZoneKind() {
        return zoneKind;
    }

    public void setZoneKind(String zoneKind) {
        this.zoneKind = zoneKind;
    }

    public Boolean getExact() {
        return exact;
    }

    public void setExact(Boolean exact) {
        this.exact = exact;
    }

    public String getViewerLayer() {
        return viewerLayer;
    }

    public void setViewerLayer(String viewerLayer) {
        this.viewerLayer = viewerLayer;
    }

    public List<String> getViewerZoneNos() {
        return viewerZoneNos;
    }

    public void setViewerZoneNos(List<String> viewerZoneNos) {
        this.viewerZoneNos = viewerZoneNos;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }
}
