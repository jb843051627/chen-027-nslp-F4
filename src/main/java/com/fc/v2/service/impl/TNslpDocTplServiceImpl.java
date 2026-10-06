package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpDocIssueMapper;
import com.fc.v2.mapper.auto.TNslpDocTplMapper;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.model.auto.TNslpDocIssue;
import com.fc.v2.model.auto.TNslpDocTpl;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.custom.DocIssueRequest;
import com.fc.v2.service.ITNslpDocTplService;
import com.fc.v2.service.support.DocVersionRules;
import com.fc.v2.util.DateUtils;
import com.fc.v2.util.StringUtils;

/**
 * 执法文书模板版次与出文 Service业务层处理。
 *
 * 选版口径全部收在 {@link DocVersionRules}：本类一次取齐该文种的版本全集，
 * 由口径处挑出文那一刻开口的唯一一版；「按版次取值」与「整表取优」两个入口
 * 都汇到同一处，两答必同版，页面不许再算第二遍。
 *
 * 旧文照它当初那一版钉成快照留在出文档案上；新立一版只供新文，不回改旧档。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Service
public class TNslpDocTplServiceImpl implements ITNslpDocTplService {

    @javax.annotation.Resource
    private TNslpDocTplMapper nslpDocTplMapper;

    @javax.annotation.Resource
    private TNslpDocIssueMapper nslpDocIssueMapper;

    @javax.annotation.Resource
    private TNslpPointMapper nslpPointMapper;

    @Override
    public TNslpDocTpl selectById(Long id) {
        if (id == null) {
            return null;
        }
        // 已撤版照主键仍查得着，只是不进现行可选清单
        return this.nslpDocTplMapper.selectById(id);
    }

    @Override
    public List<TNslpDocTpl> listActive(Date at) {
        if (at == null) {
            return new ArrayList<TNslpDocTpl>();
        }
        List<TNslpDocTpl> active = new ArrayList<TNslpDocTpl>();
        for (TNslpDocTpl tpl : snapshot()) {
            if (DocVersionRules.effectiveAt(tpl, at)) {
                active.add(tpl);
            }
        }
        Collections.sort(active, orderByNameVer());
        return active;
    }

    @Override
    public List<TNslpDocTpl> listActiveByName(String tplName, Date at) {
        List<TNslpDocTpl> all = listActive(at);
        List<TNslpDocTpl> hit = new ArrayList<TNslpDocTpl>();
        for (TNslpDocTpl tpl : all) {
            if (tplName != null && tplName.equals(tpl.getTplName())) {
                hit.add(tpl);
            }
        }
        return hit;
    }

    @Override
    public TNslpDocTpl resolveVersion(String tplName, Date at) {
        if (!DocVersionRules.isKnownName(tplName) || at == null) {
            return null;
        }
        // 一次取齐该文种全部版本（含已撤版，由口径处按窗口与情形判），交唯一选版口
        return DocVersionRules.pickVersion(snapshotByName(tplName), tplName, at);
    }

    @Override
    public TNslpDocTpl resolveTop(String tplName, Date at) {
        if (!DocVersionRules.isKnownName(tplName) || at == null) {
            return null;
        }
        // 整表取优仍汇到同一个选版口：同一刻两位置问同一文种，两答必给同一版
        return DocVersionRules.pickVersion(snapshot(), tplName, at);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpDocTpl saveVersion(TNslpDocTpl tpl) {
        return saveVersion(tpl, currentLoginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpDocTpl saveVersion(TNslpDocTpl tpl, String operator) {
        if (tpl == null) {
            throw new IllegalArgumentException("版次报文没递上来，这一版立不成");
        }
        if (!DocVersionRules.isKnownName(tpl.getTplName())) {
            throw new IllegalArgumentException(
                    "文种只认三个：" + DocVersionRules.NAME_NOTICE + "·"
                            + DocVersionRules.NAME_ORDER + "·" + DocVersionRules.NAME_REVIEW);
        }
        List<String> errors = DocVersionRules.countErrors(tpl.getVarNum(), tpl.getFillNum());
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(DocVersionRules.join(errors));
        }
        if (tpl.getEffStart() == null) {
            throw new IllegalArgumentException("起用那一一日空着，这一版何时开口定不下来");
        }
        if (tpl.getVerNo() == null) {
            throw new IllegalArgumentException("版次数值空着，版次数值大的先开口，没数没法排");
        }
        // 还差几项不留手填格子：写库前由唯一口径重轧，盖掉报文/上一回落库的那一个
        tpl.setMissNum(Integer.valueOf(DocVersionRules.miss(tpl.getVarNum(), tpl.getFillNum())));
        if (tpl.getStatus() == null) {
            tpl.setStatus(Integer.valueOf(DocVersionRules.STATUS_ACTIVE));
        }
        tpl.setDelFlag(0);
        Date now = new Date();
        if (tpl.getId() == null) {
            tpl.setCreateBy(blankToNull(operator));
            tpl.setCreateTime(now);
            this.nslpDocTplMapper.insert(tpl);
        } else {
            tpl.setUpdateBy(blankToNull(operator));
            tpl.setUpdateTime(now);
            this.nslpDocTplMapper.updateById(tpl);
        }
        return tpl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int withdrawVersion(Long id) {
        return withdrawVersion(id, currentLoginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int withdrawVersion(Long id, String operator) {
        TNslpDocTpl tpl = id == null ? null : this.nslpDocTplMapper.selectById(id);
        if (tpl == null || (tpl.getDelFlag() != null && tpl.getDelFlag() == 1)) {
            throw new IllegalArgumentException("库里没有这一版：" + id);
        }
        // 撤版只改情形：不进现行可选清单，但行不抹，照主键仍查得着，已出的旧文不动
        tpl.setStatus(Integer.valueOf(DocVersionRules.STATUS_WITHDRAWN));
        tpl.setUpdateBy(blankToNull(operator));
        tpl.setUpdateTime(new Date());
        return this.nslpDocTplMapper.updateById(tpl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpDocIssue issue(DocIssueRequest request) {
        String operator;
        try {
            operator = com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        } catch (RuntimeException e) {
            operator = null;
        }
        return issue(request, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpDocIssue issue(DocIssueRequest request, String operator) {
        if (request == null || request.getPointId() == null) {
            throw new IllegalArgumentException("出文问话没指到点位行，这一纸出不来");
        }
        if (!DocVersionRules.isKnownName(request.getTplName())) {
            throw new IllegalArgumentException(
                    "文种只认三个：" + DocVersionRules.NAME_NOTICE + "·"
                            + DocVersionRules.NAME_ORDER + "·" + DocVersionRules.NAME_REVIEW);
        }
        TNslpPoint point = this.nslpPointMapper.selectById(request.getPointId());
        if (point == null || (point.getDelFlag() != null && point.getDelFlag() == 1)) {
            throw new IllegalArgumentException("库里没有这一行点位（或已撤）：" + request.getPointId());
        }
        Date at = DateUtils.parseDate(request.getIssuedAt());
        if (at == null) {
            at = new Date();
        }

        // 选版只由服务端这一处给出：按版次取值的同一唯一口
        TNslpDocTpl tpl = resolveVersion(request.getTplName(), at);
        if (tpl == null) {
            throw new IllegalStateException("「" + request.getTplName() + "」在出文那一刻 "
                    + DateUtils.dateTimeNow(DateUtils.YYYY_MM_DD_HH_MM_SS)
                    + " 没有在位的版，这一纸出不来（已撤版不进现行清单）");
        }

        // 应填项一项项点名：点位题名空着、限值线没挂上……缺一项都不许发空白纸
        List<String> missing = DocVersionRules.missingItems(tpl, point);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(DocVersionRules.join(missing));
        }

        // 齐了才落档：三项数以出文那一刻实际取到的值重数一遍，钉成快照
        int fill = DocVersionRules.countFilled(tpl, point);
        TNslpDocIssue issue = new TNslpDocIssue();
        issue.setPointId(point.getId());
        issue.setPointNo(point.getPointNo());
        issue.setTplId(tpl.getId());
        issue.setTplNo(tpl.getTplNo());
        issue.setTplName(tpl.getTplName());
        issue.setVerNo(tpl.getVerNo());
        issue.setVarNum(tpl.getVarNum());
        issue.setFillNum(Integer.valueOf(fill));
        issue.setMissNum(Integer.valueOf(DocVersionRules.miss(tpl.getVarNum().intValue(), fill)));
        issue.setIssuedAt(at);
        issue.setDelFlag(0);
        issue.setCreateBy(blankToNull(operator));
        issue.setCreateTime(new Date());
        this.nslpDocIssueMapper.insert(issue);
        return issue;
    }

    @Override
    public List<TNslpDocIssue> listIssues(Long pointId) {
        if (pointId == null) {
            return new ArrayList<TNslpDocIssue>();
        }
        // 旧文照它当初那一版留档，按时序摆；新版不回改这里头的任何一纸
        return this.nslpDocIssueMapper.selectList(new QueryWrapper<TNslpDocIssue>()
                .eq("point_id", pointId)
                .eq("del_flag", 0)
                .orderByAsc("issued_at", "id"));
    }

    @Override
    public TNslpDocIssue selectIssueById(Long id) {
        return id == null ? null : this.nslpDocIssueMapper.selectById(id);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private List<TNslpDocTpl> snapshot() {
        return this.nslpDocTplMapper.selectList(
                new QueryWrapper<TNslpDocTpl>().eq("del_flag", 0));
    }

    private List<TNslpDocTpl> snapshotByName(String tplName) {
        return this.nslpDocTplMapper.selectList(new QueryWrapper<TNslpDocTpl>()
                .eq("del_flag", 0).eq("tpl_name", tplName));
    }

    private Comparator<TNslpDocTpl> orderByNameVer() {
        return new Comparator<TNslpDocTpl>() {
            @Override
            public int compare(TNslpDocTpl a, TNslpDocTpl b) {
                String na = a.getTplName() == null ? "" : a.getTplName();
                String nb = b.getTplName() == null ? "" : b.getTplName();
                int c = na.compareTo(nb);
                if (c != 0) {
                    return c;
                }
                int va = a.getVerNo() == null ? Integer.MIN_VALUE : a.getVerNo();
                int vb = b.getVerNo() == null ? Integer.MIN_VALUE : b.getVerNo();
                return vb - va;
            }
        };
    }

    private String blankToNull(String s) {
        return StringUtils.isNotEmpty(s) ? s.trim() : null;
    }

    private String currentLoginName() {
        try {
            return com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
