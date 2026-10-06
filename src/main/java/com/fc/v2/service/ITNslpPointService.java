package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.custom.LevelQuery;
import com.fc.v2.model.custom.LimitLineVerdict;
import com.fc.v2.model.custom.PointRegister;
import com.fc.v2.model.custom.RosterQuery;
import com.fc.v2.model.custom.RosterResult;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 声环境监测点位底册 Service接口
 *
 * 建档（建行/改行）、在走三步、名册三摆法都只认本接口给出的答案。
 * 旧的 {@link #insertTNslpPoint}／{@link #updateTNslpPoint} 收什么回什么维持原状，
 * 新规矩另开 {@link #registerPoint} 一条等价去处承接，不动旧法本体。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITNslpPointService {

    /** 按主键查询 */
    TNslpPoint selectTNslpPointById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TNslpPoint> selectTNslpPointList(Wrapper<TNslpPoint> queryWrapper);

    /** 新增（旧法，维持原状） */
    int insertTNslpPoint(TNslpPoint record);

    /** 修改（旧法，维持原状） */
    int updateTNslpPoint(TNslpPoint record);

    /** 批量删除（旧法，维持原状） */
    int deleteTNslpPointByIds(String ids);

    /** 按主键删除（旧法，维持原状） */
    int deleteTNslpPointById(Long id);

    /**
     * 点位归层（新方法承接，不动旧签名）：问一枚折算读数，走限值线那一份唯一算法。
     * 点位行上挂着线代号就按线代号取值，没挂走整表取优；
     * 归到 1..4 层就把开口线的线代号与起用那一一日钉进点位行两格；
     * 贴不上层或压根没有在位线，回话只有「不归层」，两格清成空白，不拿邻线数值凑答复。
     */
    LimitLineVerdict pinPointLevel(Long pointId, BigDecimal input, Date at, String objKind);

    /**
     * 一批点位一次回算、一次钉线：大屏昼段夜段折线、折线上的点值、后端逐条回算
     * 消费同一批发话（库只照一面，算法只走一遍）。问话里用 pointNo 认点位。
     */
    List<LimitLineVerdict> pinPointLevelBatch(List<LevelQuery> queries);

    /**
     * 看板上挂着线代号、回到 at 那一刻却寻不到在位版本的点位，一条条列出来
     * （撤下线照主键仍点得到，但不替它回话；挂码与在位版本对不上即列出）。
     */
    List<TNslpPoint> listPinnedOrphans(Date at);

    // ------------------------------------------------------------------
    // 建档：五道筛（新规矩只开这一条去处，建档岗与复测核收带出共用）
    // ------------------------------------------------------------------

    /**
     * 建行／改行唯一入口。五道筛依次过，次序不许颠倒，前一道没过不进后一道，
     * 各道筛各报各的错（抛 LedgerRejectException，带筛号与点名的栏），
     * 不合并成一句「数据不对」：
     * ①点位码（同年同册同码且仍在册即拒，指自己按更新办）→
     * ②挂靠区册（代号查无点名代号栏；撤出段不收新行；冗余全称以区册为准改齐）→
     * ③数量（逐栏点名；还差几回每回写库前重轧，盖掉上回落库值）→
     * ④同点位在走名额（没压卡、没叫停，第二行开不出来）→
     * ⑤落笔人（建/改都点得出人，指不出按没办完处理）。
     * 建档岗一条一条建与复测业务核收后带出走这同一处，界面不长第二种样子；
     * 旧纸簿行也走这同一套算法，不因建档迟早分岔。
     */
    TNslpPoint registerPoint(PointRegister register, String operator);

    /** 建档岗/复测无显式落笔人时的等价入口：落笔人取当前登录岗 */
    TNslpPoint registerPoint(PointRegister register);

    /** 往前只踏紧邻的下一步：待测→已测→压卡；压卡即到头，动不得、撤不掉 */
    TNslpPoint advanceWalk(Long id, String operator);

    /** 推进（落笔人取当前登录岗） */
    TNslpPoint advanceWalk(Long id);

    /** 已测可退回一步重来；先前那一趟留痕照旧留着，不抹、不并排 */
    TNslpPoint rollbackWalk(Long id, String operator);

    /** 收回重来（落笔人取当前登录岗） */
    TNslpPoint rollbackWalk(Long id);

    /** 就地叫停：腾出同点位名额，这一行在名册上照常露脸但不再动手 */
    TNslpPoint haltPoint(Long id, String operator, String reason);

    /** 就地叫停（落笔人取当前登录岗） */
    TNslpPoint haltPoint(Long id, String reason);

    /** 这一行眼下走到第几步：由系统依已走留痕步数数出，屏上写的、人记的不作依据 */
    Integer currentStep(Long id);

    /** 压卡行整行撤不掉；撤行只接没压卡、没叫停的行 */
    int removePoint(Long id, String operator);

    /** 撤行（落笔人取当前登录岗） */
    int removePoint(Long id);

    // ------------------------------------------------------------------
    // 摆法：分页、三搜索、区层可见范围、三种视图同一份底数
    // ------------------------------------------------------------------

    /**
     * 名册唯一摆法口。页长取区局配置（问话改不了）；三处搜索（点位码、区册代号、
     * 区层类别）前缀与全等都认；本层值守岗只见本层，区局岗四层都见但限划归的册；
     * 按区层类别／按区册代号／按入册情形三种视图处数同一底数。
     * 本年度一行没建时留空是合法结果，不拿邻层数据凑数。
     */
    RosterResult roster(RosterQuery query);
}
