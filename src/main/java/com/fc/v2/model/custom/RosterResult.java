package com.fc.v2.model.custom;

import java.util.List;

import com.fc.v2.model.auto.TNslpPoint;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 名册摆法回话：行、总数、页长与三种视图分组同出一处。
 *
 * 同一年度同一批行，三种视图（按区层类别／按区册代号／按入册情形）给出的处数
 * 必须一样——前端数一趟、数据库数一趟都不作数，只认这一处同一份底数写回的数。
 * 页首那几个数（在册处数、在走处数、本页处数）也钉在这一处。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "RosterResult", description = "名册摆法回话")
public class RosterResult implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 本页行（只呈现库里真实存在的行） */
    @ApiModelProperty(value = "本页行")
    private List<TNslpPoint> rows;

    /** 底数总数（三种视图处数都以这个为准） */
    @ApiModelProperty(value = "底数总数")
    private long total;

    /** 当前页（从 1 起） */
    @ApiModelProperty(value = "当前页")
    private int pageNum;

    /** 每页几条（区局配置那一处的值，问话改不了） */
    @ApiModelProperty(value = "每页几条")
    private int pageSize;

    /** 在册处数：入了册的行都算（压卡的、叫停的、撤出段名下旧行、旧纸簿行一并算） */
    @ApiModelProperty(value = "在册处数")
    private int registeredNum;

    /** 在走处数：没压卡、没叫停的行（旧纸簿在走行也算，留空不当压卡） */
    @ApiModelProperty(value = "在走处数")
    private int walkingNum;

    /** 视图分组标签（同一批行三种摆法，组内处数相加等于底数总数） */
    @ApiModelProperty(value = "视图分组")
    private List<Group> groups;

    /** 视图里的一组：分组名＋该组处数（不另查第二份底数，只在同一份行集合上归类） */
    public static final class Group {
        private final String key;
        private final int num;

        public Group(String key, int num) {
            this.key = key;
            this.num = num;
        }

        public String getKey() {
            return key;
        }

        public int getNum() {
            return num;
        }
    }

    public List<TNslpPoint> getRows() {
        return rows;
    }

    public void setRows(List<TNslpPoint> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getRegisteredNum() {
        return registeredNum;
    }

    public void setRegisteredNum(int registeredNum) {
        this.registeredNum = registeredNum;
    }

    public int getWalkingNum() {
        return walkingNum;
    }

    public void setWalkingNum(int walkingNum) {
        this.walkingNum = walkingNum;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
