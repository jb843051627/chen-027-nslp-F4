package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 出文问话：照哪一行点位、出哪一个文种，在什么一刻出。
 *
 * 版次不由问话指定——本轮该照哪一版，由服务端按出文那一刻唯一选出，
 * 取值也只由服务端一处给出。
 *
 * @author fuce
 * @date 2026-10-06
 */
@ApiModel(value = "DocIssueRequest", description = "执法文书出文问话")
public class DocIssueRequest implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 照哪一行点位出文 */
    @ApiModelProperty(value = "点位行主键")
    private Long pointId;

    /** 文种（超标告知·责令限期改正·复查结论） */
    @ApiModelProperty(value = "文种")
    private String tplName;

    /** 出文那一刻（不填按此刻）；选版窗口与缺项点名都看这一刻 */
    @ApiModelProperty(value = "出文那一刻 yyyy-MM-dd HH:mm:ss")
    private String issuedAt;

    public Long getPointId() {
        return pointId;
    }

    public void setPointId(Long pointId) {
        this.pointId = pointId;
    }

    public String getTplName() {
        return tplName;
    }

    public void setTplName(String tplName) {
        this.tplName = tplName;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }
}
