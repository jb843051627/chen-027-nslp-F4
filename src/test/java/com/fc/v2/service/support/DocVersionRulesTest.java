package com.fc.v2.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TNslpDocTpl;
import com.fc.v2.model.auto.TNslpPoint;

/**
 * 文书版次口径测试：窗口选版（版次大先开口）、撤版出局但主键可查在服务层验、
 * 三项数轧算、出文缺项逐项点名。
 */
public class DocVersionRulesTest {

    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Date t(String s) {
        try {
            return F.parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TNslpDocTpl tpl(Integer id, Integer ver, String name, Integer status,
                                   String start, String end) {
        TNslpDocTpl t = new TNslpDocTpl();
        t.setId(id == null ? null : Long.valueOf(id.longValue()));
        t.setVerNo(ver);
        t.setTplName(name);
        t.setStatus(status);
        t.setEffStart(t(start));
        t.setEffEnd(end == null ? null : t(end));
        t.setDelFlag(0);
        return t;
    }

    @Test
    public void pickHighestVersionInWindow() {
        TNslpDocTpl v1 = tpl(1, 1, "超标告知", 0, "2024-01-01 00:00:00", "2025-01-01 00:00:00");
        TNslpDocTpl v2 = tpl(2, 2, "超标告知", 0, "2025-01-01 00:00:00", "2026-01-01 00:00:00");
        TNslpDocTpl v3 = tpl(3, 3, "超标告知", 0, "2026-01-01 00:00:00", null);
        List<TNslpDocTpl> all = java.util.Arrays.asList(v1, v2, v3);

        // 2025-06 在线的只有第 2 版
        TNslpDocTpl mid = DocVersionRules.pickVersion(all, "超标告知", t("2025-06-01 00:00:00"));
        assertEquals(Long.valueOf(2L), mid.getId());
        // 2026 在线的是第 3 版（交班留空=一直用下去）
        TNslpDocTpl now = DocVersionRules.pickVersion(all, "超标告知", t("2026-10-06 00:00:00"));
        assertEquals(Long.valueOf(3L), now.getId());
        // 起用头含：恰好压在起用日，新版开口
        TNslpDocTpl edge = DocVersionRules.pickVersion(all, "超标告知", t("2025-01-01 00:00:00"));
        assertEquals(Long.valueOf(2L), edge.getId());
        // v2 交班与 v3 起用同一刻：旧版尾不含已交班、新版头含已起用，由新版 v3 开口
        TNslpDocTpl edge2 = DocVersionRules.pickVersion(all, "超标告知", t("2026-01-01 00:00:00"));
        assertEquals(Long.valueOf(3L), edge2.getId());
        // 别个文种不串
        assertNull(DocVersionRules.pickVersion(all, "复查结论", t("2026-10-06 00:00:00")));
    }

    @Test
    public void withdrawnVersionExcluded() {
        TNslpDocTpl active = tpl(1, 1, "超标告知", 0, "2026-01-01 00:00:00", null);
        TNslpDocTpl withdrawn = tpl(2, 9, "超标告知", 1, "2026-01-01 00:00:00", null);
        // 版次再大，记成已撤版也不进开口
        TNslpDocTpl picked = DocVersionRules.pickVersion(
                java.util.Arrays.asList(active, withdrawn), "超标告知", t("2026-10-06 00:00:00"));
        assertEquals(Long.valueOf(1L), picked.getId());
        assertFalse(DocVersionRules.effectiveAt(withdrawn, t("2026-10-06 00:00:00")));
    }

    @Test
    public void threeCountsAndMiss() {
        assertTrue(DocVersionRules.countErrors(null, 1).get(0).contains("应填项数"));
        assertTrue(DocVersionRules.countErrors(-1, 0).get(0).contains("应填项数"));
        assertTrue(DocVersionRules.countErrors(2, 3).get(0).contains("反超"));
        assertTrue(DocVersionRules.countErrors(2, 1).isEmpty());
        assertEquals(2, DocVersionRules.miss(5, 3));
        assertEquals(0, DocVersionRules.miss(5, 5));
    }

    @Test
    public void missingItemsNamedOneByOne() {
        TNslpDocTpl tpl = tpl(1, 1, "超标告知", 0, "2026-01-01 00:00:00", null);
        tpl.setVarNum(2);

        // 两项都空：点名点位题名、限值线代号，一纸出不来
        TNslpPoint empty = new TNslpPoint();
        List<String> m1 = DocVersionRules.missingItems(tpl, empty);
        assertEquals(2, m1.size());
        assertTrue(m1.get(0).contains("点位题名"));
        assertTrue(m1.get(1).contains("限值线代号"));
        assertEquals(0, DocVersionRules.countFilled(tpl, empty));

        // 题名有、挂线空：只点限值线
        TNslpPoint half = new TNslpPoint();
        half.setContent("临江片区1号点");
        List<String> m2 = DocVersionRules.missingItems(tpl, half);
        assertEquals(1, m2.size());
        assertTrue(m2.get(0).contains("限值线代号"));
        assertEquals(1, DocVersionRules.countFilled(tpl, half));

        // 两项都齐：出得来
        TNslpPoint full = new TNslpPoint();
        full.setContent("临江片区1号点");
        full.setPinnedRuleCode("X-01");
        assertTrue(DocVersionRules.missingItems(tpl, full).isEmpty());
        assertEquals(2, DocVersionRules.countFilled(tpl, full));
    }
}
