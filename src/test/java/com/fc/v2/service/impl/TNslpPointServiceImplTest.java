package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.mapper.auto.TNslpPointTrailMapper;
import com.fc.v2.mapper.auto.TNslpZoneBookMapper;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.auto.TNslpPointTrail;
import com.fc.v2.model.auto.TNslpZoneBook;
import com.fc.v2.model.custom.PointRegister;
import com.fc.v2.model.custom.RosterQuery;
import com.fc.v2.model.custom.RosterResult;
import com.fc.v2.service.ITNslpLimitLineService;
import com.fc.v2.service.support.LedgerRejectException;
import com.fc.v2.service.support.PointLedgerRules;

/**
 * 建档五筛、在走三步、压卡冻结、旧行兼容、名册三摆法同一底数的内存账测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TNslpPointServiceImplTest {

    @Mock
    private TNslpPointMapper baseMapper;
    @Mock
    private TNslpZoneBookMapper zoneBookMapper;
    @Mock
    private TNslpPointTrailMapper trailMapper;
    @Mock
    private ITNslpLimitLineService limitLineService;
    @InjectMocks
    private TNslpPointServiceImpl service;

    private List<TNslpPoint> pointStore;
    private List<TNslpZoneBook> zoneStore;
    private List<TNslpPointTrail> trailStore;
    private long idSeq;

    private static final Pattern CLAUSE =
            Pattern.compile("(\\w+)\\s*(<>|<=|>=|=|<|>)\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}");
    private static final Pattern IS_NULL = Pattern.compile("(\\w+)\\s+IS\\s+NULL", Pattern.CASE_INSENSITIVE);

    private static boolean eqv(Object a, Object b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.equals(b) || String.valueOf(a).equals(String.valueOf(b));
    }

    private Object fieldOf(TNslpPoint p, String col) {
        if ("point_no".equals(col)) {
            return p.getPointNo();
        }
        if ("annual".equals(col)) {
            return p.getAnnual();
        }
        if ("zone_no".equals(col)) {
            return p.getZoneNo();
        }
        if ("del_flag".equals(col)) {
            return p.getDelFlag();
        }
        if ("id".equals(col)) {
            return p.getId();
        }
        if ("walk_status".equals(col)) {
            return p.getWalkStatus();
        }
        if ("halted".equals(col)) {
            return p.getHalted();
        }
        return null;
    }

    private boolean pointMatches(TNslpPoint p, String seg, Map<String, Object> params) {
        Matcher m = CLAUSE.matcher(seg);
        while (m.find()) {
            Object val = params.get(m.group(3));
            Object f = fieldOf(p, m.group(1));
            boolean ok;
            if ("<>".equals(m.group(2))) {
                ok = !eqv(f, val);
            } else {
                ok = "=".equals(m.group(2)) ? eqv(f, val) : false;
            }
            if (!ok) {
                return false;
            }
        }
        Matcher n = IS_NULL.matcher(seg);
        while (n.find()) {
            if (fieldOf(p, n.group(1)) != null) {
                return false;
            }
        }
        return true;
    }

    @BeforeEach
    public void setUp() {
        pointStore = new ArrayList<TNslpPoint>();
        zoneStore = new ArrayList<TNslpZoneBook>();
        trailStore = new ArrayList<TNslpPointTrail>();
        idSeq = 0;
        service.setRosterPageSize(2);

        zoneStore.add(book("QN00", "云桥临江甲册", "甲层", 0));
        zoneStore.add(book("QN02", "云桥工业丙册", "丙层", 0));
        zoneStore.add(book("QN01", "南津北岸旧册（已撤出）", "乙层", 1));

        when(zoneBookMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TNslpZoneBook>(zoneStore));
        when(zoneBookMapper.selectOne(any())).thenAnswer(inv -> {
            QueryWrapper<?> w = inv.getArgument(0);
            w.getSqlSegment();
            String zoneNo = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof String) {
                    zoneNo = (String) v;
                }
            }
            for (TNslpZoneBook b : zoneStore) {
                if (java.util.Objects.equals(b.getZoneNo(), zoneNo)
                        && (b.getDelFlag() == null || b.getDelFlag() == 0)) {
                    return b;
                }
            }
            return null;
        });

        when(baseMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<TNslpPoint> w = inv.getArgument(0);
            String seg = w.getSqlSegment() == null ? "" : w.getSqlSegment();
            Map<String, Object> params = w.getParamNameValuePairs();
            List<TNslpPoint> out = new ArrayList<TNslpPoint>();
            for (TNslpPoint p : pointStore) {
                if (pointMatches(p, seg, params)) {
                    out.add(p);
                }
            }
            return out;
        });
        when(baseMapper.selectCount(any())).thenAnswer(inv -> {
            QueryWrapper<TNslpPoint> w = inv.getArgument(0);
            String seg = w.getSqlSegment() == null ? "" : w.getSqlSegment();
            int n = 0;
            for (TNslpPoint p : pointStore) {
                if (pointMatches(p, seg, w.getParamNameValuePairs())) {
                    n++;
                }
            }
            return Integer.valueOf(n);
        });
        when(baseMapper.selectById(any())).thenAnswer(inv -> {
            Long id = Long.valueOf(inv.getArgument(0).toString());
            for (TNslpPoint p : pointStore) {
                if (p.getId().equals(id)) {
                    return p;
                }
            }
            return null;
        });
        when(baseMapper.insert(any(TNslpPoint.class))).thenAnswer(inv -> {
            TNslpPoint p = inv.getArgument(0);
            p.setId(Long.valueOf(++idSeq));
            pointStore.add(p);
            return 1;
        });
        when(baseMapper.updateById(any(TNslpPoint.class))).thenAnswer(inv -> {
            TNslpPoint upd = inv.getArgument(0);
            for (TNslpPoint p : pointStore) {
                if (p.getId().equals(upd.getId())) {
                    copyPoint(upd, p);
                }
            }
            return 1;
        });
        when(baseMapper.update(any(), any(Wrapper.class))).thenAnswer(inv -> {
            // removePoint 的软删：按包装里的 id 找到行，置 del_flag=1
            com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> w = inv.getArgument(1);
            String seg = w.getSqlSegment() == null ? "" : w.getSqlSegment();
            for (TNslpPoint p : pointStore) {
                if (pointMatches(p, seg, w.getParamNameValuePairs())) {
                    p.setDelFlag(1);
                }
            }
            return 1;
        });

        when(trailMapper.insert(any(TNslpPointTrail.class))).thenAnswer(inv -> {
            TNslpPointTrail t = inv.getArgument(0);
            t.setId(Long.valueOf(++idSeq));
            trailStore.add(t);
            return 1;
        });
        when(trailMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<TNslpPointTrail> w = inv.getArgument(0);
            w.getSqlSegment();
            Long pointId = null;
            for (Object v : w.getParamNameValuePairs().values()) {
                if (v instanceof Number || (v != null && String.valueOf(v).matches("\\d+"))) {
                    pointId = Long.valueOf(String.valueOf(v));
                }
            }
            List<TNslpPointTrail> out = new ArrayList<TNslpPointTrail>();
            for (TNslpPointTrail t : trailStore) {
                if (pointId == null || pointId.equals(t.getPointId())) {
                    out.add(t);
                }
            }
            return out;
        });
    }

    private static TNslpZoneBook book(String no, String name, String kind, int status) {
        TNslpZoneBook b = new TNslpZoneBook();
        b.setId(Long.valueOf((long) no.hashCode() & 0x7fffffff));
        b.setZoneNo(no);
        b.setZoneName(name);
        b.setZoneKind(kind);
        b.setStatus(status);
        b.setDelFlag(0);
        return b;
    }

    private static void copyPoint(TNslpPoint src, TNslpPoint dst) {
        if (src.getPointNo() != null) {
            dst.setPointNo(src.getPointNo());
        }
        dst.setAnnual(src.getAnnual());
        dst.setZoneId(src.getZoneId());
        if (src.getZoneNo() != null) {
            dst.setZoneNo(src.getZoneNo());
        }
        if (src.getZoneName() != null) {
            dst.setZoneName(src.getZoneName());
        }
        if (src.getNeedNum() != null) {
            dst.setNeedNum(src.getNeedNum());
        }
        if (src.getGotNum() != null) {
            dst.setGotNum(src.getGotNum());
        }
        if (src.getLackNum() != null) {
            dst.setLackNum(src.getLackNum());
        }
        if (src.getWalkStatus() != null) {
            dst.setWalkStatus(src.getWalkStatus());
        }
        if (src.getHalted() != null) {
            dst.setHalted(src.getHalted());
        }
        if (src.getContent() != null) {
            dst.setContent(src.getContent());
        }
        if (src.getSrcKind() != null) {
            dst.setSrcKind(src.getSrcKind());
        }
        if (src.getUpdateBy() != null) {
            dst.setUpdateBy(src.getUpdateBy());
        }
        if (src.getUpdateTime() != null) {
            dst.setUpdateTime(src.getUpdateTime());
        }
        if (src.getRemark() != null) {
            dst.setRemark(src.getRemark());
        }
        if (src.getDelFlag() != null) {
            dst.setDelFlag(src.getDelFlag());
        }
    }

    private static PointRegister reg(String pointNo, Integer annual, String zoneNo,
                                     String need, String got) {
        PointRegister r = new PointRegister();
        r.setPointNo(pointNo);
        r.setAnnual(annual);
        r.setZoneNo(zoneNo);
        r.setNeedNum(need);
        r.setGotNum(got);
        return r;
    }

    // ------------------------------------------------------------------
    // 五道筛
    // ------------------------------------------------------------------

    @Test
    public void screen1CodeAndDup() {
        TNslpPoint p1 = service.registerPoint(reg("PT0001", 2026, "QN00", "30", "12"), "建档员甲");
        // 30 配 12 得 18，重轧结果落库
        assertEquals(Integer.valueOf(18), p1.getLackNum());
        assertEquals("云桥临江甲册", p1.getZoneName());
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_PENDING), p1.getWalkStatus());
        assertEquals("建档员甲", p1.getCreateBy());
        assertNotNull(p1.getCreateTime());

        // 同年同册同码仍在册：建行拒掉，且报的是第 1 道筛
        LedgerRejectException dup = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0001", 2026, "QN00", "5", "0"), "建档员乙"));
        assertEquals(1, dup.getScreen());

        // 第一行还在走：名额只认点位不认年度，同年异册或同年换维护单位都挡在第 4 道
        LedgerRejectException blocked = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0001", 2025, "QN00", "5", "0"), "建档员乙"));
        assertEquals(4, blocked.getScreen());

        // 指自己（同 id）按更新办，不许被同码挡在门外（走着也能改自己的数量）
        PointRegister self = reg("PT0001", 2026, "QN00", "20", "7");
        self.setId(p1.getId());
        TNslpPoint updated = service.registerPoint(self, "建档员乙");
        // 20 配 7 得 13：更正后重轧盖掉上回 18
        assertEquals(Integer.valueOf(13), updated.getLackNum());
        assertEquals("建档员乙", updated.getUpdateBy());

        // 压卡腾出名额后，不同年度的同码行才立得起来（且同年同册去重管不到异年）
        service.advanceWalk(p1.getId(), "建档员甲");
        service.advanceWalk(p1.getId(), "建档员甲");
        TNslpPoint otherYear = service.registerPoint(reg("PT0001", 2025, "QN00", "5", "0"), "建档员乙");
        assertNotNull(otherYear);

        // 点位码空着也栽在第 1 道
        LedgerRejectException blank = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("  ", 2026, "QN00", "5", "0"), "建档员甲"));
        assertEquals(1, blank.getScreen());
    }

    @Test
    public void screen2Zone() {
        // 代号底里查无：点名代号这一栏
        LedgerRejectException noZone = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0002", 2026, "QN99", "5", "0"), "建档员甲"));
        assertEquals(2, noZone.getScreen());
        assertTrue(noZone.getMessage().contains("区册代号"));
        // 已撤出的段不再收新行
        LedgerRejectException withdrawn = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0003", 2026, "QN01", "5", "0"), "建档员甲"));
        assertEquals(2, withdrawn.getScreen());
        assertTrue(withdrawn.getMessage().contains("撤出"));
    }

    @Test
    public void screen3QuantityNamesColumn() {
        LedgerRejectException e1 = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0010", 2026, "QN00", null, "1"), "甲"));
        assertEquals(3, e1.getScreen());
        assertTrue(e1.getMessage().contains("核定应测次数"));

        LedgerRejectException e2 = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0010", 2026, "QN00", "5", "6"), "甲"));
        assertEquals(3, e2.getScreen());
        assertTrue(e2.getMessage().contains("反超"));

        // 已达标空着按 0：5 配 0 得 5，过得
        TNslpPoint p = service.registerPoint(reg("PT0010", 2026, "QN00", "5", null), "甲");
        assertEquals(Integer.valueOf(5), p.getLackNum());
    }

    @Test
    public void screen4WalkingSlot() {
        service.registerPoint(reg("PT0020", 2026, "QN00", "5", "0"), "甲");
        // 两家维护单位同日各报 / 换人重报：一点位不许叠两行（哪怕换了册也不行，名额认点位）
        LedgerRejectException blocked = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0020", 2026, "QN02", "5", "0"), "乙"));
        assertEquals(4, blocked.getScreen());

        // 压卡腾名额后，同点位可立新行
        TNslpPoint first = pointStore.get(0);
        service.advanceWalk(first.getId(), "甲");
        service.advanceWalk(first.getId(), "甲");
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_SEALED), first.getWalkStatus());
        TNslpPoint second = service.registerPoint(reg("PT0020", 2026, "QN02", "9", "9"), "乙");
        // 两数一样过得 0
        assertEquals(Integer.valueOf(0), second.getLackNum());
    }

    @Test
    public void screen5Operator() {
        LedgerRejectException e = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PT0030", 2026, "QN00", "5", "0"), "  "));
        assertEquals(5, e.getScreen());
    }

    @Test
    public void zoneNameDriftCorrectedOnUpdate() {
        TNslpPoint p = service.registerPoint(reg("PT0040", 2026, "QN00", "5", "0"), "甲");
        // 库里行上冗余全称被改歪，改行时以区册底为准改齐，缺陷留备注
        p.setZoneName("不知哪年抄歪的旧称");
        PointRegister r = reg("PT0040", 2026, "QN00", "5", "1");
        r.setId(p.getId());
        TNslpPoint fixed = service.registerPoint(r, "乙");
        assertEquals("云桥临江甲册", fixed.getZoneName());
        assertTrue(fixed.getRemark().contains("以区册底为准改齐"));
        // 5 配 1 得 4（改挂/更正后第三趟重轧同样打这一处过）
        assertEquals(Integer.valueOf(4), fixed.getLackNum());
    }

    // ------------------------------------------------------------------
    // 在走三步
    // ------------------------------------------------------------------

    @Test
    public void walkAdvanceRollbackSealFreeze() {
        TNslpPoint p = service.registerPoint(reg("PT0050", 2026, "QN00", "10", "0"), "甲");
        // 步由留痕数：建行后在待测
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_PENDING), service.currentStep(p.getId()));

        service.advanceWalk(p.getId(), "甲");
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_MEASURED), service.currentStep(p.getId()));
        // 待测退无可退（此刻在已测可退）；先退
        service.rollbackWalk(p.getId(), "甲");
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_PENDING), service.currentStep(p.getId()));
        // 先前那一趟的记录照旧留着：建行、推进、收回三笔都在，不抹、不并排
        assertEquals(3, trailStore.size());

        // 待测退无可退
        assertThrows(IllegalStateException.class, () -> service.rollbackWalk(p.getId(), "甲"));

        // 一路到压卡
        service.advanceWalk(p.getId(), "甲");
        service.advanceWalk(p.getId(), "甲");
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_SEALED), service.currentStep(p.getId()));
        // 压卡后：推不动、收不回、撤不掉、改不动
        assertThrows(IllegalStateException.class, () -> service.advanceWalk(p.getId(), "甲"));
        assertThrows(IllegalStateException.class, () -> service.rollbackWalk(p.getId(), "甲"));
        assertThrows(IllegalStateException.class, () -> service.removePoint(p.getId(), "甲"));
        PointRegister edit = reg("PT0050", 2026, "QN00", "11", "0");
        edit.setId(p.getId());
        assertThrows(IllegalStateException.class, () -> service.registerPoint(edit, "甲"));
    }

    @Test
    public void haltFreesSlotButKeepsRow() {
        TNslpPoint p = service.registerPoint(reg("PT0060", 2026, "QN00", "10", "0"), "甲");
        service.haltPoint(p.getId(), "甲", "维护单位换人重报");
        assertEquals(Integer.valueOf(PointLedgerRules.HALTED), p.getHalted());
        // 叫停腾出名额：第二行开得出来；第一行仍在册可列
        TNslpPoint p2 = service.registerPoint(reg("PT0060", 2026, "QN00", "8", "2"), "乙");
        assertNotNull(p2);
        assertEquals(2, pointStore.size());
        // 叫停的行不再接改动
        assertThrows(IllegalStateException.class, () -> service.advanceWalk(p.getId(), "甲"));
    }

    @Test
    public void legacyPaperRowsTolerated() {
        // 旧纸簿搬进来：步数、点位码核验串全空（这里 annual 空、walk_status 空）
        TNslpPoint legacy = new TNslpPoint();
        legacy.setPointNo("PTOLD01");
        legacy.setZoneNo("QN00");
        legacy.setZoneName("云桥临江甲册");
        legacy.setNeedNum(12);
        legacy.setGotNum(3);
        legacy.setLackNum(99);
        legacy.setDelFlag(0);
        pointStore.add(legacy);
        legacy.setId(Long.valueOf(++idSeq));

        // 不能一碰就报错；步数空着不等于压卡
        assertNull(service.currentStep(legacy.getId()));
        assertFalse(PointLedgerRules.isSealed(legacy.getWalkStatus()));
        // 在走的旧行占名额：同点新行被第 4 道筛挡下
        LedgerRejectException blocked = assertThrows(LedgerRejectException.class,
                () -> service.registerPoint(reg("PTOLD01", 2026, "QN00", "5", "0"), "甲"));
        assertEquals(4, blocked.getScreen());
        // 旧行也能从待测这一步走起，且 lack 立刻按同一套算法重轧
        service.advanceWalk(legacy.getId(), "甲");
        assertEquals(Integer.valueOf(PointLedgerRules.WALK_MEASURED), service.currentStep(legacy.getId()));
    }

    // ------------------------------------------------------------------
    // 名册三摆法
    // ------------------------------------------------------------------

    @Test
    public void rosterSameBaseAcrossViewsScopeSearchPaging() {
        // 本年度 4 行：甲层 QN00 三条（一条压卡）、乙层撤出册 QN01 一条；丙层 QN02 一条
        service.registerPoint(reg("PT1001", 2026, "QN00", "10", "0"), "甲");
        service.registerPoint(reg("PT1002", 2026, "QN00", "10", "0"), "甲");
        TNslpPoint sealMe = service.registerPoint(reg("PT1003", 2026, "QN00", "10", "0"), "甲");
        service.advanceWalk(sealMe.getId(), "甲");
        service.advanceWalk(sealMe.getId(), "甲");
        TNslpPoint oldSeg = service.registerPoint(reg("PT2001", 2026, "QN02", "10", "0"), "甲");
        // QN01 是已撤出册：register 不收，直接造一条并走前的旧行
        TNslpPoint withdrawnSeg = new TNslpPoint();
        withdrawnSeg.setPointNo("PT3001");
        withdrawnSeg.setAnnual(2026);
        withdrawnSeg.setZoneNo("QN01");
        withdrawnSeg.setZoneName("南津北岸旧册（已撤出）");
        withdrawnSeg.setNeedNum(4);
        withdrawnSeg.setGotNum(0);
        withdrawnSeg.setLackNum(4);
        withdrawnSeg.setWalkStatus(0);
        withdrawnSeg.setHalted(0);
        withdrawnSeg.setDelFlag(0);
        pointStore.add(withdrawnSeg);
        withdrawnSeg.setId(Long.valueOf(++idSeq));

        // 三种视图同一底数：总数都是 5，各组处数相加也是 5
        RosterQuery q = new RosterQuery();
        q.setAnnual(2026);
        for (String view : Arrays.asList("byKind", "byZone", "byIntake")) {
            q.setView(view);
            RosterResult r = service.roster(q);
            assertEquals(5, r.getTotal(), view);
            assertEquals(5, r.getRegisteredNum(), view);
            int sum = 0;
            for (RosterResult.Group g : r.getGroups()) {
                sum += g.getNum();
            }
            assertEquals(5, sum, view);
        }
        // 在走处数：压卡那条不占在走，其余 4 处在走（撤出段旧行也算）
        RosterResult all = service.roster(q);
        assertEquals(4, all.getWalkingNum());

        // 页长认配置（=2）：第一页 2 条，能翻页
        assertEquals(2, all.getPageSize());
        assertEquals(2, all.getRows().size());
        RosterQuery page3 = new RosterQuery();
        page3.setAnnual(2026);
        page3.setPageNum(3);
        assertEquals(1, service.roster(page3).getRows().size());

        // 搜索：前缀与全等都认
        RosterQuery prefix = new RosterQuery();
        prefix.setAnnual(2026);
        prefix.setPointNo("PT10");
        assertEquals(3, service.roster(prefix).getTotal());
        RosterQuery exact = new RosterQuery();
        exact.setAnnual(2026);
        exact.setPointNo("PT1001");
        exact.setExact(Boolean.TRUE);
        assertEquals(1, service.roster(exact).getTotal());
        RosterQuery zonePrefix = new RosterQuery();
        zonePrefix.setAnnual(2026);
        zonePrefix.setZoneNo("QN0");
        assertEquals(5, service.roster(zonePrefix).getTotal());

        // 区层类别搜（前缀/全等）
        RosterQuery kind = new RosterQuery();
        kind.setAnnual(2026);
        kind.setZoneKind("甲");
        assertEquals(3, service.roster(kind).getTotal());

        // 值守岗只见本层
        RosterQuery guard = new RosterQuery();
        guard.setAnnual(2026);
        guard.setViewerLayer("丙层");
        assertEquals(1, service.roster(guard).getTotal());
        RosterQuery guardNone = new RosterQuery();
        guardNone.setAnnual(2026);
        guardNone.setViewerLayer("丁层");
        assertEquals(0, service.roster(guardNone).getTotal());

        // 区局岗四层都见，但限划归的册：只给 QN00
        RosterQuery bureau = new RosterQuery();
        bureau.setAnnual(2026);
        bureau.setViewerZoneNos(Arrays.asList("QN00"));
        assertEquals(3, service.roster(bureau).getTotal());

        // 本年度一行没建：留空是合法结果，不拿邻年凑
        RosterQuery empty = new RosterQuery();
        empty.setAnnual(2099);
        RosterResult er = service.roster(empty);
        assertEquals(0, er.getTotal());
        assertTrue(er.getRows().isEmpty());
        assertTrue(er.getGroups().isEmpty());
    }
}
