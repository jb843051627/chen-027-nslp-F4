package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpDocIssueMapper;
import com.fc.v2.mapper.auto.TNslpDocTplMapper;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.model.auto.TNslpDocIssue;
import com.fc.v2.model.auto.TNslpDocTpl;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.custom.DocIssueRequest;
import com.fc.v2.service.support.DocVersionRules;

/**
 * 文书版次服务测试：两入口同版、撤版出局主键可查、缺项点名、旧文快照不回改。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TNslpDocTplServiceImplTest {

    @Mock
    private TNslpDocTplMapper tplMapper;
    @Mock
    private TNslpDocIssueMapper issueMapper;
    @Mock
    private TNslpPointMapper pointMapper;
    @InjectMocks
    private TNslpDocTplServiceImpl service;

    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Date t(String s) {
        try {
            return F.parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<TNslpDocTpl> tplStore;
    private List<TNslpDocIssue> issueStore;
    private List<TNslpPoint> pointStore;
    private long idSeq;

    private static final Pattern EQ = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

    @BeforeEach
    public void setUp() {
        tplStore = new ArrayList<TNslpDocTpl>();
        issueStore = new ArrayList<TNslpDocIssue>();
        pointStore = new ArrayList<TNslpPoint>();
        idSeq = 100;

        when(tplMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<TNslpDocTpl> w = inv.getArgument(0);
            String seg = w.getSqlSegment() == null ? "" : w.getSqlSegment();
            Map<String, Object> params = w.getParamNameValuePairs();
            List<TNslpDocTpl> out = new ArrayList<TNslpDocTpl>();
            for (TNslpDocTpl d : tplStore) {
                boolean ok = true;
                Matcher m = EQ.matcher(seg);
                while (m.find()) {
                    Object v = params.get(m.group(2));
                    Object f = "tpl_name".equals(m.group(1)) ? d.getTplName()
                            : "del_flag".equals(m.group(1)) ? d.getDelFlag() : null;
                    if (f == null || !String.valueOf(f).equals(String.valueOf(v))) {
                        ok = false;
                    }
                }
                if (ok) {
                    out.add(d);
                }
            }
            return out;
        });
        when(tplMapper.selectById(any())).thenAnswer(inv -> {
            Long id = Long.valueOf(inv.getArgument(0).toString());
            for (TNslpDocTpl d : tplStore) {
                if (d.getId().equals(id)) {
                    return d;
                }
            }
            return null;
        });
        when(tplMapper.insert(any(TNslpDocTpl.class))).thenAnswer(inv -> {
            TNslpDocTpl d = inv.getArgument(0);
            d.setId(Long.valueOf(++idSeq));
            tplStore.add(d);
            return 1;
        });
        when(tplMapper.updateById(any(TNslpDocTpl.class))).thenAnswer(inv -> {
            TNslpDocTpl upd = inv.getArgument(0);
            for (TNslpDocTpl d : tplStore) {
                if (d.getId().equals(upd.getId())) {
                    d.setStatus(upd.getStatus());
                    d.setMissNum(upd.getMissNum());
                    d.setFillNum(upd.getFillNum());
                    d.setUpdateBy(upd.getUpdateBy());
                    d.setUpdateTime(upd.getUpdateTime());
                }
            }
            return 1;
        });

        when(pointMapper.selectById(any())).thenAnswer(inv -> {
            Long id = Long.valueOf(inv.getArgument(0).toString());
            for (TNslpPoint p : pointStore) {
                if (p.getId().equals(id)) {
                    return p;
                }
            }
            return null;
        });
        when(issueMapper.insert(any(TNslpDocIssue.class))).thenAnswer(inv -> {
            TNslpDocIssue i = inv.getArgument(0);
            i.setId(Long.valueOf(++idSeq));
            issueStore.add(i);
            return 1;
        });
        when(issueMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TNslpDocIssue>(issueStore));
    }

    private TNslpDocTpl version(long id, int ver, String name, int status, int varNum,
                                String start, String end) {
        TNslpDocTpl d = new TNslpDocTpl();
        d.setId(Long.valueOf(id));
        d.setTplNo(name + "-TPL");
        d.setTplName(name);
        d.setVerNo(ver);
        d.setVarNum(varNum);
        d.setFillNum(varNum);
        d.setMissNum(0);
        d.setStatus(status);
        d.setEffStart(t(start));
        d.setEffEnd(end == null ? null : t(end));
        d.setDelFlag(0);
        tplStore.add(d);
        return d;
    }

    private TNslpPoint point(long id, boolean name, boolean rule) {
        TNslpPoint p = new TNslpPoint();
        p.setId(Long.valueOf(id));
        p.setPointNo("PT" + id);
        if (name) {
            p.setContent("临江片区" + id + "号点");
        }
        if (rule) {
            p.setPinnedRuleCode("R-" + id);
        }
        p.setDelFlag(0);
        pointStore.add(p);
        return p;
    }

    @Test
    public void twoEntriesGiveSameVersion() {
        version(1, 1, "超标告知", 0, 2, "2024-01-01 00:00:00", "2026-01-01 00:00:00");
        version(2, 2, "超标告知", 0, 2, "2026-01-01 00:00:00", null);
        version(3, 1, "复查结论", 0, 2, "2024-01-01 00:00:00", null);

        Date at = t("2026-10-06 00:00:00");
        TNslpDocTpl byName = service.resolveVersion("超标告知", at);
        TNslpDocTpl byTop = service.resolveTop("超标告知", at);
        assertNotNull(byName);
        assertEquals(Long.valueOf(2L), byName.getId());
        assertEquals(byName.getId(), byTop.getId());
        // 现行清单里第 1 版已交班、不在；第 2 版与复查结论第 1 版在
        assertEquals(2, service.listActive(at).size());
        // 无在线版
        assertNull(service.resolveVersion("责令限期改正", at));
    }

    @Test
    public void withdrawnExcludedButStillSelectableById() {
        version(1, 1, "超标告知", 0, 2, "2026-01-01 00:00:00", null);
        TNslpDocTpl bad = version(2, 9, "超标告知", 1, 2, "2026-01-01 00:00:00", null);
        // 开口的是在位第 1 版，不是版次更大的已撤版
        assertEquals(Long.valueOf(1L), service.resolveVersion("超标告知", t("2026-10-06 00:00:00")).getId());
        assertTrue(service.listActive(t("2026-10-06 00:00:00")).size() == 1);
        // 撤版照主键仍查得着
        assertEquals(Long.valueOf(2L), service.selectById(bad.getId()).getId());
    }

    @Test
    public void saveRecomputesMissAndValidates() {
        TNslpDocTpl d = new TNslpDocTpl();
        d.setTplName("复查结论");
        d.setVerNo(1);
        d.setVarNum(5);
        d.setFillNum(3);
        d.setEffStart(t("2026-01-01 00:00:00"));
        TNslpDocTpl saved = service.saveVersion(d, "制版员");
        // 还差几项=5-3，由服务端轧后盖掉
        assertEquals(Integer.valueOf(2), saved.getMissNum());
        assertEquals("制版员", saved.getCreateBy());

        TNslpDocTpl bad = new TNslpDocTpl();
        bad.setTplName("复查结论");
        bad.setVerNo(2);
        bad.setVarNum(2);
        bad.setFillNum(3);
        bad.setEffStart(t("2026-01-01 00:00:00"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> service.saveVersion(bad, "制版员")).getMessage().contains("反超"));
    }

    @Test
    public void issueNamesMissingItemsAndSnapshotsVersion() {
        // 第 1 版旧年用；第 2 版本年开口
        version(1, 1, "超标告知", 0, 2, "2024-01-01 00:00:00", "2026-01-01 00:00:00");
        version(2, 2, "超标告知", 0, 2, "2026-01-01 00:00:00", null);
        // 题名、限值线都齐的点位
        TNslpPoint ready = point(1, true, true);
        // 只齐题名、没挂线的点位
        TNslpPoint noRule = point(2, true, false);

        DocIssueRequest req = new DocIssueRequest();
        req.setPointId(ready.getId());
        req.setTplName("超标告知");
        req.setIssuedAt("2026-10-06 00:00:00");
        TNslpDocIssue issue = service.issue(req, "出文员");
        // 照的是此刻开口的第 2 版，三项数钉成快照
        assertEquals(Integer.valueOf(2), issue.getVerNo());
        assertEquals(Long.valueOf(2L), issue.getTplId());
        assertEquals(Integer.valueOf(2), issue.getVarNum());
        assertEquals(Integer.valueOf(2), issue.getFillNum());
        assertEquals(Integer.valueOf(0), issue.getMissNum());
        assertEquals("出文员", issue.getCreateBy());

        // 缺限值线：一项项点名，一纸出不来
        DocIssueRequest bad = new DocIssueRequest();
        bad.setPointId(noRule.getId());
        bad.setTplName("超标告知");
        bad.setIssuedAt("2026-10-06 00:00:00");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.issue(bad, "出文员"));
        assertTrue(ex.getMessage().contains("限值线代号"));
        // 没出成，不增档案
        assertEquals(1, issueStore.size());

        // 往后新立第 3 版并撤第 2 版：旧文档案仍钉在第 2 版，不回改
        version(3, 3, "超标告知", 0, 2, "2027-01-01 00:00:00", null);
        List<TNslpDocIssue> archived = service.listIssues(ready.getId());
        assertEquals(1, archived.size());
        assertEquals(Integer.valueOf(2), archived.get(0).getVerNo());

        // 在旧一刻（2025）出文仍照第 1 版——补算旧账不拿此刻最新版套
        DocIssueRequest old = new DocIssueRequest();
        old.setPointId(ready.getId());
        old.setTplName("超标告知");
        old.setIssuedAt("2025-06-01 00:00:00");
        TNslpDocIssue oldIssue = service.issue(old, "出文员");
        assertEquals(Integer.valueOf(1), oldIssue.getVerNo());
    }
}
