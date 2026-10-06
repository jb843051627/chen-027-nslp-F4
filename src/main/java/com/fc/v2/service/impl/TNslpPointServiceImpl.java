package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TNslpPointMapper;
import com.fc.v2.mapper.auto.TNslpPointTrailMapper;
import com.fc.v2.mapper.auto.TNslpZoneBookMapper;
import com.fc.v2.model.auto.TNslpLimitLine;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.auto.TNslpPointTrail;
import com.fc.v2.model.auto.TNslpZoneBook;
import com.fc.v2.model.custom.LevelQuery;
import com.fc.v2.model.custom.LimitLineVerdict;
import com.fc.v2.model.custom.PointRegister;
import com.fc.v2.model.custom.RosterQuery;
import com.fc.v2.model.custom.RosterResult;
import com.fc.v2.service.ITNslpLimitLineService;
import com.fc.v2.service.ITNslpPointService;
import com.fc.v2.service.support.LedgerRejectException;
import com.fc.v2.service.support.LimitLineRules;
import com.fc.v2.service.support.PointLedgerRules;
import com.fc.v2.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 声环境监测点位底册 Service业务层处理。
 *
 * 建档 {@link #registerPoint} 按五道筛依次过，口径钉在 {@link PointLedgerRules}，
 * 本类只落库与装配，不另起算法；在走三步（待测／已测／压卡）的步数由留痕那一串
 * 放出来；名册 {@link #roster} 是分页、搜索、区层可见范围与三种视图的唯一摆法口。
 *
 * 旧的 {@link #insertTNslpPoint}／{@link #updateTNslpPoint} 收什么回什么维持原状；
 * 新规矩另开 registerPoint 一条等价去处承接，不动旧法本体。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TNslpPointServiceImpl extends ServiceImpl<TNslpPointMapper, TNslpPoint> implements ITNslpPointService {

    /** 名册每页几条：区局写在配置里，问话改不了 */
    @Value("${fuce.nslp.page-size:10}")
    private int rosterPageSize;

    @Autowired
    private TNslpZoneBookMapper nslpZoneBookMapper;

    @Autowired
    private TNslpPointTrailMapper nslpPointTrailMapper;

    @Autowired
    private ITNslpLimitLineService nslpLimitLineService;

    /** 测试与装配处换页长用（页长仍只认配置这一处来源） */
    void setRosterPageSize(int rosterPageSize) {
        this.rosterPageSize = rosterPageSize;
    }

    @Override
    public TNslpPoint selectTNslpPointById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TNslpPoint> selectTNslpPointList(Wrapper<TNslpPoint> queryWrapper) {
        QueryWrapper<TNslpPoint> wrapper = new QueryWrapper<TNslpPoint>();
        com.github.pagehelper.PageHelper.startPage(1, 10);
        wrapper.eq("status", 0);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTNslpPoint(TNslpPoint record) {
        if (record == null) {
            return 0;
        }

        record.setCreateBy(record.getPointNo());
        TNslpZoneBook refArch = nslpZoneBookMapper.selectOne(new QueryWrapper<TNslpZoneBook>()
                .eq("id", record.getZoneId()).eq("del_flag", 0));
        if (refArch == null) {
            return 0;
        }
        if (refArch.getStatus() != null && refArch.getStatus() == 1) {
            return 0;
        }
        record.setZoneNo(refArch.getZoneNo());
        if (StringUtils.isNotEmpty(record.getPointNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TNslpPoint>()
                    .eq("point_no", record.getPointNo()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }
        int remIn = record.getNeedNum() == null ? 0 : record.getNeedNum();
        int remOut = record.getGotNum() == null ? 0 : record.getGotNum();
        record.setLackNum(remIn + remOut);

        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTNslpPoint(TNslpPoint record) {
        if (record == null || record.getId() == null) {
            return 0;
        }

        if (record.getId() != null && StringUtils.isNotEmpty(record.getPointNo())) {
            Integer dupCnt = this.baseMapper.selectCount(new QueryWrapper<TNslpPoint>()
                    .eq("point_no", record.getPointNo()).ne("id", record.getId()).eq("del_flag", 0));
            if (dupCnt != null && dupCnt > 0) {
                return 0;
            }
        }

        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TNslpPoint>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int deleteTNslpPointByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTNslpPointById(Long id) {
        return this.baseMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    // 建档：五道筛
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint registerPoint(PointRegister register) {
        return registerPoint(register, currentLoginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint registerPoint(PointRegister register, String operator) {
        if (register == null) {
            throw new LedgerRejectException(1, "建档报文没递上来，一行册立不起来");
        }

        // 第五道筛的落笔人先备着，但判定仍按次序在最后一道落下
        String actor = operator == null ? null : operator.trim();
        boolean update = register.getId() != null;

        TNslpPoint self = null;
        if (update) {
            self = mustGetPoint(register.getId());
            // 压卡行到头：添不进、字动不得、整行撤不掉；要改只能另起新的一行
            if (PointLedgerRules.isSealed(self.getWalkStatus())) {
                throw new IllegalStateException(
                        "这一行已压卡，字动不得、整行撤不掉；要改只能另起新的一行：" + self.getId());
            }
            if (self.getHalted() != null && self.getHalted() == PointLedgerRules.HALTED) {
                throw new IllegalStateException("这一行已就地叫停，不再接改动：" + self.getId());
            }
        }

        // —— 第一道筛：点位码（随件进来，不另发、不核构成；只挡同年同册同码仍在册的另一条）——
        String pointNo = register.getPointNo() == null ? null : register.getPointNo().trim();
        if (PointLedgerRules.codeBlank(pointNo)) {
            throw new LedgerRejectException(1, "点位码这一栏空着；点位码随报送材料进来，这一行进不了库");
        }
        Integer annual = register.getAnnual();
        String submittedZoneNo = register.getZoneNo() == null ? "" : register.getZoneNo().trim();
        if (duplicateRegistered(pointNo, annual, submittedZoneNo, register.getId())) {
            throw new LedgerRejectException(1,
                    "同一年度（" + annual + "）同一区册（" + submittedZoneNo
                            + "）底下，库里已有一条同码「" + pointNo + "」且仍在册的行，这次建行拒掉");
        }

        // —— 第二道筛：挂靠（错落在代号这一栏；撤出段不收新行；冗余全称以区册为准改齐）——
        if (PointLedgerRules.zoneBlank(submittedZoneNo)) {
            throw new LedgerRejectException(2, "区册代号这一栏对不上：代号空着，区册底里找不着这一册");
        }
        TNslpZoneBook book = nslpZoneBookMapper.selectOne(new QueryWrapper<TNslpZoneBook>()
                .eq("zone_no", submittedZoneNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (book == null) {
            throw new LedgerRejectException(2,
                    "区册代号这一栏对不上：区册底里查无代号「" + submittedZoneNo + "」这一册，建行建不成");
        }
        if (book.getStatus() != null && book.getStatus().intValue() == 1) {
            throw new LedgerRejectException(2,
                    "区册「" + submittedZoneNo + "」这一段已并走撤出，不再收新行；并走前的旧行照旧在册");
        }

        // —— 第三道筛：数量（逐栏点名；lack 每回写库之前由唯一口径重轧）——
        List<String> qtyErrors = PointLedgerRules.quantityErrors(register.getNeedNum(), register.getGotNum());
        if (!qtyErrors.isEmpty()) {
            throw new LedgerRejectException(3, PointLedgerRules.join(qtyErrors));
        }
        int need = Integer.parseInt(register.getNeedNum().trim());
        int got = PointLedgerRules.parseOrNull(register.getGotNum()) == null
                ? 0 : Integer.parseInt(register.getGotNum().trim());
        int lack = PointLedgerRules.lack(need, got);

        // —— 第四道筛：同一处点位同时只容一行在走（自己改行不挡自己）——
        TNslpPoint blocker = findWalkingBlocker(pointNo, register.getId());
        if (blocker != null) {
            throw new LedgerRejectException(4,
                    "同一处点位「" + pointNo + "」已有一行没压卡、也没叫停（行主键 " + blocker.getId()
                            + "），第二行开不出来；等它压卡或叫停腾出名额再接");
        }

        // —— 第五道筛：落笔人（建/改都点得出是哪一位）——
        if (PointLedgerRules.operatorBlank(actor)) {
            throw new LedgerRejectException(5,
                    "指不出是哪一位" + (update ? "改的这行" : "建的册") + "，按没办完处理，这一行落不了库");
        }

        // 五道筛过完，落库
        Date now = new Date();
        boolean rezoned = self != null
                && self.getZoneNo() != null && !book.getZoneNo().equals(self.getZoneNo());
        TNslpPoint row = self == null ? new TNslpPoint() : self;
        row.setPointNo(pointNo);
        row.setAnnual(annual);
        row.setZoneId(Integer.valueOf(book.getId().intValue()));
        row.setZoneNo(book.getZoneNo());
        // 行上冗余全称跟区册底对不齐：以区册为准把这一行改过来。
        // 比的是库里这一行原存的那一份（新建时行上还没有，照底册填齐，不算缺陷）；
        // 两处各留一份说法的旧行，改齐同时把这笔缺陷留在备注上。
        if (self != null && PointLedgerRules.zoneNameDrifts(self.getZoneName(), book.getZoneName())) {
            appendRemark(row, "区册全称以区册底为准改齐（行上原存「" + self.getZoneName()
                    + "」→ 底册「" + book.getZoneName() + "」），两处各留一份说法算缺陷");
        }
        row.setZoneName(book.getZoneName());
        row.setNeedNum(Integer.valueOf(need));
        row.setGotNum(Integer.valueOf(got));
        // 重算三趟（头一次写入／已达标被更正／改挂别册）都打这一处过：盖掉上一回落库的那一个
        row.setLackNum(Integer.valueOf(lack));
        if (register.getContent() != null) {
            row.setContent(register.getContent().trim());
        }
        row.setSrcKind(StringUtils.isNotEmpty(register.getSrcKind())
                ? register.getSrcKind().trim() : PointLedgerRules.SRC_MANUAL);
        row.setDelFlag(0);
        if (self == null) {
            // 新行从待测走起；旧纸簿行另由存量导入，不在这一处
            row.setWalkStatus(Integer.valueOf(PointLedgerRules.WALK_PENDING));
            row.setHalted(Integer.valueOf(PointLedgerRules.NOT_HALTED));
            if (row.getStatus() == null) {
                row.setStatus(Integer.valueOf(0));
            }
            row.setCreateBy(actor);
            row.setCreateTime(now);
            this.baseMapper.insert(row);
            addTrail(row.getId(), PointLedgerRules.WALK_PENDING, PointLedgerRules.ACT_CREATE,
                    "建行：点位" + pointNo + "，年度" + annual + "，区册" + book.getZoneNo(), now);
        } else {
            row.setUpdateBy(actor);
            row.setUpdateTime(now);
            this.baseMapper.updateById(row);
            if (rezoned) {
                // 改挂不挪步数，只在身后留一句；步数仍由推进/收回那一串放出来
                addTrail(row.getId(), stepOrZero(row), "改挂",
                        "改挂到区册" + book.getZoneNo() + "，数量按新行重轧：还差" + lack + "回", now);
            }
        }
        return row;
    }

    // ------------------------------------------------------------------
    // 在走三步：步由留痕那一串数出来
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint advanceWalk(Long id) {
        return advanceWalk(id, currentLoginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint advanceWalk(Long id, String operator) {
        TNslpPoint row = mustGetPoint(id);
        String actor = requireActor(operator, "推进");
        if (PointLedgerRules.isSealed(row.getWalkStatus())) {
            throw new IllegalStateException("这一行已压卡，到了头，再添不进去：" + id);
        }
        if (row.getHalted() != null && row.getHalted() == PointLedgerRules.HALTED) {
            throw new IllegalStateException("这一行已就地叫停，推不动：" + id);
        }
        Integer from = currentStep(id);
        int step = from == null ? PointLedgerRules.WALK_PENDING : from.intValue();
        if (!PointLedgerRules.canAdvance(step)) {
            throw new IllegalStateException("步子不许乱踏：当前「" + PointLedgerRules.walkName(from)
                    + "」没有紧邻的下一步：" + id);
        }
        int to = step + 1;
        boolean seal = to == PointLedgerRules.WALK_SEALED;
        Date now = new Date();
        row.setWalkStatus(Integer.valueOf(to));
        row.setUpdateBy(actor);
        row.setUpdateTime(now);
        this.baseMapper.updateById(row);
        addTrail(id, to, seal ? PointLedgerRules.ACT_SEAL : PointLedgerRules.ACT_STEP,
                seal ? "压卡：这一行到头，添不进、动不得、撤不掉" : "推进到「" + PointLedgerRules.walkName(to) + "」",
                now);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint rollbackWalk(Long id) {
        return rollbackWalk(id, currentLoginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint rollbackWalk(Long id, String operator) {
        TNslpPoint row = mustGetPoint(id);
        String actor = requireActor(operator, "收回重来");
        if (PointLedgerRules.isSealed(row.getWalkStatus())) {
            throw new IllegalStateException("已压卡的行收不回来：" + id);
        }
        if (row.getHalted() != null && row.getHalted() == PointLedgerRules.HALTED) {
            throw new IllegalStateException("已叫停的行收不回来：" + id);
        }
        Integer from = currentStep(id);
        int step = from == null ? PointLedgerRules.WALK_PENDING : from.intValue();
        if (!PointLedgerRules.canRollback(step)) {
            throw new IllegalStateException("只有已测退得回待测；当前停在「"
                    + PointLedgerRules.walkName(from) + "」：" + id);
        }
        int to = step - 1;
        Date now = new Date();
        row.setWalkStatus(Integer.valueOf(to));
        row.setUpdateBy(actor);
        row.setUpdateTime(now);
        this.baseMapper.updateById(row);
        // 先前那一趟的记录照旧留着，不抹、不跟后补的并排；新的一趟从头再数
        addTrail(id, to, PointLedgerRules.ACT_BACK, "已测退回一步重来，先前那一趟留痕照旧留着", now);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint haltPoint(Long id, String reason) {
        return haltPoint(id, currentLoginName(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TNslpPoint haltPoint(Long id, String operator, String reason) {
        TNslpPoint row = mustGetPoint(id);
        String actor = requireActor(operator, "叫停");
        if (PointLedgerRules.isSealed(row.getWalkStatus())) {
            throw new IllegalStateException("已压卡的行不必再叫停：" + id);
        }
        if (row.getHalted() != null && row.getHalted() == PointLedgerRules.HALTED) {
            return row;
        }
        Date now = new Date();
        row.setHalted(Integer.valueOf(PointLedgerRules.HALTED));
        row.setUpdateBy(actor);
        row.setUpdateTime(now);
        this.baseMapper.updateById(row);
        addTrail(id, stepOrZero(row), PointLedgerRules.ACT_HALT,
                "就地叫停，腾出同点位名额" + (StringUtils.isNotEmpty(reason) ? "：" + reason.trim() : ""), now);
        return row;
    }

    @Override
    public Integer currentStep(Long id) {
        TNslpPoint row = mustGetPoint(id);
        // 眼下第几步由系统依已走步数自行数出；屏上写的档、经办人记的档都不作依据
        Integer byTrail = PointLedgerRules.replaySteps(listTrailActions(id));
        if (byTrail != null) {
            return byTrail;
        }
        // 旧纸簿搬进来的行：步数空着，不能一碰就报错，更不能当成已经压卡
        return row.getWalkStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removePoint(Long id) {
        return removePoint(id, currentLoginName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removePoint(Long id, String operator) {
        TNslpPoint row = mustGetPoint(id);
        if (PointLedgerRules.isSealed(row.getWalkStatus())) {
            throw new IllegalStateException("已压卡的行整行撤不掉，要改只能另起新行：" + id);
        }
        if (row.getHalted() != null && row.getHalted() == PointLedgerRules.HALTED) {
            throw new IllegalStateException("已叫停的行留在册上，不撤：" + id);
        }
        String actor = requireActor(operator, "撤行");
        UpdateWrapper<TNslpPoint> uw = new UpdateWrapper<TNslpPoint>()
                .eq("id", id).eq("del_flag", 0)
                .set("del_flag", 1)
                .set("update_by", actor)
                .set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    // ------------------------------------------------------------------
    // 摆法：分页 / 三搜索 / 区层范围 / 三视图同一底数
    // ------------------------------------------------------------------

    @Override
    public RosterResult roster(RosterQuery query) {
        RosterQuery q = query == null ? new RosterQuery() : query;
        // 本年度一行没建时留空是合法结果；年度没给，不拿隔年的行凑
        if (q.getAnnual() == null) {
            return emptyRoster(q);
        }

        // 底数只取这一份：入了册的行（del_flag=0），本视图年度。
        // 年度传 0 专门摆旧纸簿行（annual 空着那批），它们在对应视图里一样查得着、计入在册处数。
        QueryWrapper<TNslpPoint> base = new QueryWrapper<TNslpPoint>().eq("del_flag", 0);
        if (q.getAnnual().intValue() == 0) {
            base.isNull("annual");
        } else {
            base.eq("annual", q.getAnnual());
        }
        List<TNslpPoint> all = this.baseMapper.selectList(base);
        Map<String, TNslpZoneBook> books = zoneBookMap();

        // 可见范围按区层切：值守岗只见挂靠本层的行；区局岗四层都见但限划归的册
        List<TNslpPoint> scoped = new ArrayList<TNslpPoint>();
        Set<String> zoneScope = q.getViewerZoneNos() == null ? null : new HashSet<String>(q.getViewerZoneNos());
        for (TNslpPoint p : all) {
            TNslpZoneBook book = books.get(p.getZoneNo());
            if (!visible(q.getViewerLayer(), zoneScope, book)) {
                continue;
            }
            // 三处搜索：点位码、区册代号、区层类别；前缀匹配与全等匹配都认
            if (!matchCode(p.getPointNo(), q.getPointNo(), Boolean.TRUE.equals(q.getExact()))) {
                continue;
            }
            if (!matchCode(p.getZoneNo(), q.getZoneNo(), Boolean.TRUE.equals(q.getExact()))) {
                continue;
            }
            String kind = book == null ? null : book.getZoneKind();
            if (!matchCode(kind, q.getZoneKind(), Boolean.TRUE.equals(q.getExact()))) {
                continue;
            }
            scoped.add(p);
        }
        sortRows(scoped, books);

        // 三种视图在同一份行集合上归类：组内处数相加必等于底数总数，不另查第二份底数
        List<RosterResult.Group> groups = groupBy(scoped, books, q.getView());

        int pageSize = rosterPageSize > 0 ? rosterPageSize : 10;
        int pageNum = q.getPageNum() == null || q.getPageNum().intValue() < 1 ? 1 : q.getPageNum();
        int from = Math.min((pageNum - 1) * pageSize, scoped.size());
        int to = Math.min(from + pageSize, scoped.size());

        RosterResult result = new RosterResult();
        result.setRows(new ArrayList<TNslpPoint>(scoped.subList(from, to)));
        result.setTotal(scoped.size());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        // 页首那几个数与视图处数同一份底数：前端数、数据库另数都不作数
        result.setRegisteredNum(scoped.size());
        int walking = 0;
        for (TNslpPoint p : scoped) {
            if (PointLedgerRules.occupiesSlot(p)) {
                walking++;
            }
        }
        result.setWalkingNum(walking);
        result.setGroups(groups);
        return result;
    }

    // ------------------------------------------------------------------
    // 归层（旧法，口径仍走限值线唯一裁决口，原样保留）
    // ------------------------------------------------------------------

    @Override
    public LimitLineVerdict pinPointLevel(Long pointId, BigDecimal input, Date at, String objKind) {
        TNslpPoint point = this.baseMapper.selectOne(new QueryWrapper<TNslpPoint>()
                .eq("id", pointId).eq("del_flag", 0));
        if (point == null || input == null || at == null) {
            return LimitLineVerdict.notLevel(at, LimitLineRules.alignOne(input), null);
        }
        LevelQuery q = new LevelQuery(point.getPointNo(), point.getPinnedRuleCode(), input, at);
        q.setObjKind(objKind);
        // 单个也走批量那唯一裁决口：按线代号取值与整表取优、大屏点值与后端回算，从无第二条算法
        LimitLineVerdict verdict = nslpLimitLineService.resolveBatch(java.util.Collections.singletonList(q)).get(0);
        applyVerdict(point, verdict);
        return verdict;
    }

    @Override
    public List<LimitLineVerdict> pinPointLevelBatch(List<LevelQuery> queries) {
        List<LimitLineVerdict> verdicts = nslpLimitLineService.resolveBatch(queries);
        if (queries == null || queries.isEmpty()) {
            return verdicts;
        }
        // 问话里用 pointNo 认点位；一批回话一次算完，再逐行钉线/清空白
        Map<String, TNslpPoint> pointMap = new HashMap<String, TNslpPoint>();
        for (TNslpPoint p : this.baseMapper.selectList(new QueryWrapper<TNslpPoint>().eq("del_flag", 0))) {
            if (p.getPointNo() != null) {
                pointMap.put(p.getPointNo(), p);
            }
        }
        for (int i = 0; i < queries.size() && i < verdicts.size(); i++) {
            TNslpPoint point = pointMap.get(queries.get(i).getPointNo());
            if (point != null) {
                applyVerdict(point, verdicts.get(i));
            }
        }
        return verdicts;
    }

    @Override
    public List<TNslpPoint> listPinnedOrphans(Date at) {
        List<TNslpPoint> orphans = new ArrayList<TNslpPoint>();
        if (at == null) {
            return orphans;
        }
        // at 那一刻在位且配置干净的线代号全集；现行清单里没有的挂码即孤儿
        java.util.Set<String> activeCodes = new java.util.HashSet<String>();
        for (TNslpLimitLine line : nslpLimitLineService.listAvailable(at)) {
            if (line.getRuleCode() != null) {
                activeCodes.add(line.getRuleCode());
            }
        }
        List<TNslpPoint> pinned = this.baseMapper.selectList(new QueryWrapper<TNslpPoint>()
                .eq("del_flag", 0).isNotNull("pinned_rule_code"));
        for (TNslpPoint p : pinned) {
            if (p.getPinnedRuleCode() != null && !p.getPinnedRuleCode().trim().isEmpty()
                    && !activeCodes.contains(p.getPinnedRuleCode())) {
                orphans.add(p);
            }
        }
        return orphans;
    }

    /**
     * 归到层就把线代号与起用那一一日钉进行上两格；不归层两格清成空白。
     * 拿邻线数值凑一个像样答复的事，这里一概不认。
     */
    private void applyVerdict(TNslpPoint point, LimitLineVerdict verdict) {
        UpdateWrapper<TNslpPoint> uw = new UpdateWrapper<TNslpPoint>()
                .eq("id", point.getId()).eq("del_flag", 0);
        if (verdict != null && verdict.isLeveled()) {
            uw.set("pinned_rule_code", verdict.getRuleCode())
              .set("pinned_eff_start", verdict.getEffStart());
            point.setPinnedRuleCode(verdict.getRuleCode());
            point.setPinnedEffStart(verdict.getEffStart());
        } else {
            uw.set("pinned_rule_code", null).set("pinned_eff_start", null);
            point.setPinnedRuleCode(null);
            point.setPinnedEffStart(null);
        }
        uw.set("update_time", new Date());
        this.baseMapper.update(null, uw);
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private TNslpPoint mustGetPoint(Long id) {
        if (id == null) {
            throw new LedgerRejectException(1, "行主键没给，找不到这一行");
        }
        TNslpPoint p = this.baseMapper.selectById(id);
        if (p == null || (p.getDelFlag() != null && p.getDelFlag() == 1)) {
            throw new LedgerRejectException(1, "库里没有这一行（或已撤）：" + id);
        }
        return p;
    }

    /**
     * 第一道筛的去重：同一年度同一区册底下已有一条同码、且仍占着在走名额的行。
     * 「仍在册」在这里与第四道名额同口径——压卡、叫停的行虽在册上照常露脸，
     * 但名额已腾出，不挡新行（原文「压卡之后同点位再要立新行才接得上」即此义：
     * 压卡行没删册，若仍按在册去重，新行便永远接不上）。旧纸簿行步数空着算在走。
     */
    private boolean duplicateRegistered(String pointNo, Integer annual, String zoneNo, Long selfId) {
        QueryWrapper<TNslpPoint> w = new QueryWrapper<TNslpPoint>()
                .eq("point_no", pointNo)
                .eq("zone_no", zoneNo)
                .eq("del_flag", 0);
        if (annual == null) {
            w.isNull("annual");
        } else {
            w.eq("annual", annual);
        }
        List<TNslpPoint> same = this.baseMapper.selectList(w);
        for (TNslpPoint p : same) {
            if (selfId != null && selfId.equals(p.getId())) {
                continue;
            }
            if (PointLedgerRules.occupiesSlot(p)) {
                return true;
            }
        }
        return false;
    }

    private TNslpPoint findWalkingBlocker(String pointNo, Long selfId) {
        // 同一点位只容一行在走；旧纸簿行 walk_status 留空也算在走，不把它当压卡
        List<TNslpPoint> same = this.baseMapper.selectList(new QueryWrapper<TNslpPoint>()
                .eq("point_no", pointNo)
                .eq("del_flag", 0));
        for (TNslpPoint p : same) {
            if (selfId != null && selfId.equals(p.getId())) {
                continue;
            }
            if (PointLedgerRules.occupiesSlot(p)) {
                return p;
            }
        }
        return null;
    }

    private Map<String, TNslpZoneBook> zoneBookMap() {
        Map<String, TNslpZoneBook> map = new HashMap<String, TNslpZoneBook>();
        for (TNslpZoneBook b : nslpZoneBookMapper.selectList(
                new QueryWrapper<TNslpZoneBook>().eq("del_flag", 0))) {
            if (b.getZoneNo() != null) {
                map.put(b.getZoneNo(), b);
            }
        }
        return map;
    }

    /**
     * 可见范围：值守岗（viewerLayer 非空）只见挂靠本层的行；
     * 区局岗（viewerLayer 空、zoneScope 非空）四层都见，但限划归它的那些区册；
     * 两处都没给（系统内部问话）不限。本层当季未建行就是留空，不拿邻层凑。
     */
    private boolean visible(String viewerLayer, Set<String> zoneScope, TNslpZoneBook book) {
        boolean layerGiven = viewerLayer != null && !viewerLayer.trim().isEmpty();
        boolean scopeGiven = zoneScope != null;
        if (!layerGiven && !scopeGiven) {
            return true;
        }
        if (book == null) {
            return false;
        }
        if (layerGiven) {
            if (!viewerLayer.trim().equals(book.getZoneKind())) {
                return false;
            }
            // 值守岗也只认划给本岗看的册；没列册单就按本层全册认
            return zoneScope == null || zoneScope.contains(book.getZoneNo());
        }
        return zoneScope.contains(book.getZoneNo());
    }

    /** 全等与前缀都认；问话空着表示这一处不限 */
    private boolean matchCode(String field, String keyword, boolean exact) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        if (field == null) {
            return false;
        }
        String k = keyword.trim();
        return exact ? field.equals(k) : (field.equals(k) || field.startsWith(k));
    }

    private void sortRows(List<TNslpPoint> rows, final Map<String, TNslpZoneBook> books) {
        Collections.sort(rows, new Comparator<TNslpPoint>() {
            @Override
            public int compare(TNslpPoint a, TNslpPoint b) {
                int c = str(a.getZoneNo()).compareTo(str(b.getZoneNo()));
                if (c != 0) {
                    return c;
                }
                c = str(a.getPointNo()).compareTo(str(b.getPointNo()));
                if (c != 0) {
                    return c;
                }
                Long ia = a.getId() == null ? Long.MIN_VALUE : a.getId();
                Long ib = b.getId() == null ? Long.MIN_VALUE : b.getId();
                return ia.compareTo(ib);
            }

            private String str(String s) {
                return s == null ? "" : s;
            }
        });
    }

    /**
     * 三种摆法只在同一份行集合上归类，处数相加恒等于底数总数：
     * byKind 按区层类别／byZone 按区册代号／byIntake 按入册情形（册面 0新入册 1已核齐 2缺项，旧纸簿单列）。
     */
    private List<RosterResult.Group> groupBy(List<TNslpPoint> rows, Map<String, TNslpZoneBook> books,
                                             String view) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (TNslpPoint p : rows) {
            String key;
            if ("byZone".equals(view)) {
                key = p.getZoneNo() == null ? "未挂区册" : p.getZoneNo();
            } else if ("byIntake".equals(view)) {
                key = intakeName(p.getStatus());
            } else {
                TNslpZoneBook b = books.get(p.getZoneNo());
                key = b == null || b.getZoneKind() == null ? "区层不明" : b.getZoneKind();
            }
            Integer n = counts.get(key);
            counts.put(key, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
        }
        List<RosterResult.Group> groups = new ArrayList<RosterResult.Group>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            groups.add(new RosterResult.Group(e.getKey(), e.getValue().intValue()));
        }
        return groups;
    }

    private String intakeName(Integer status) {
        if (status == null) {
            return "旧纸簿行";
        }
        if (status.intValue() == 0) {
            return "新入册";
        }
        if (status.intValue() == 1) {
            return "已核齐";
        }
        if (status.intValue() == 2) {
            return "缺项";
        }
        return "入册情形不明";
    }

    private RosterResult emptyRoster(RosterQuery q) {
        RosterResult r = new RosterResult();
        r.setRows(new ArrayList<TNslpPoint>());
        r.setTotal(0L);
        r.setPageNum(q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum());
        r.setPageSize(rosterPageSize > 0 ? rosterPageSize : 10);
        r.setRegisteredNum(0);
        r.setWalkingNum(0);
        r.setGroups(new ArrayList<RosterResult.Group>());
        return r;
    }

    private void appendRemark(TNslpPoint row, String piece) {
        String old = row.getRemark();
        row.setRemark(old == null || old.trim().isEmpty() ? piece : old + "；" + piece);
    }

    private int stepOrZero(TNslpPoint row) {
        return row.getWalkStatus() == null ? PointLedgerRules.WALK_PENDING : row.getWalkStatus().intValue();
    }

    private String requireActor(String operator, String action) {
        if (PointLedgerRules.operatorBlank(operator)) {
            throw new LedgerRejectException(5, action + "也得点得出落笔是哪一位，指不出人按没办完处理");
        }
        return operator.trim();
    }

    private String currentLoginName() {
        try {
            return com.fc.v2.shiro.util.ShiroUtils.getLoginName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<String> listTrailActions(Long pointId) {
        List<TNslpPointTrail> trails = nslpPointTrailMapper.selectList(new QueryWrapper<TNslpPointTrail>()
                .eq("point_id", pointId)
                .eq("del_flag", 0)
                .orderByAsc("trail_time", "seq", "id"));
        List<String> actions = new ArrayList<String>();
        for (TNslpPointTrail t : trails) {
            actions.add(t.getAction());
        }
        return actions;
    }

    private int addTrail(Long pointId, int step, String action, String note, Date at) {
        TNslpPointTrail t = new TNslpPointTrail();
        t.setPointId(pointId);
        t.setStep(Integer.valueOf(step));
        t.setAction(action);
        t.setNote(note);
        t.setTrailTime(at);
        t.setSeq(Integer.valueOf(nextSeq(pointId, at)));
        t.setDelFlag(Integer.valueOf(0));
        return nslpPointTrailMapper.insert(t);
    }

    private int nextSeq(Long pointId, Date at) {
        List<TNslpPointTrail> same = nslpPointTrailMapper.selectList(new QueryWrapper<TNslpPointTrail>()
                .eq("point_id", pointId)
                .eq("trail_time", at)
                .eq("del_flag", 0));
        return same.size();
    }
}
