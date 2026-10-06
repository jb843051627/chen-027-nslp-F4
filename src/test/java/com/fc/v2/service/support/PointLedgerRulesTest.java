package com.fc.v2.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.TNslpPoint;

/**
 * 建档五筛与在走步数纯口径测试：数量轧算、逐栏点名、同点名额、旧行留空、留痕放步数。
 */
public class PointLedgerRulesTest {

    private static TNslpPoint row(Integer walk, Integer halted, Integer delFlag) {
        TNslpPoint p = new TNslpPoint();
        p.setWalkStatus(walk);
        p.setHalted(halted);
        p.setDelFlag(delFlag);
        return p;
    }

    @Test
    public void lackIsNeedMinusGot() {
        // 30 配 12 得 18；20 配 7 得 13；两数一样得 0
        assertEquals(18, PointLedgerRules.lack(30, 12));
        assertEquals(13, PointLedgerRules.lack(20, 7));
        assertEquals(0, PointLedgerRules.lack(9, 9));
    }

    @Test
    public void quantityErrorsNameTheColumn() {
        // 核定没填：点核定这一栏
        assertTrue(PointLedgerRules.quantityErrors(null, "3").get(0).contains("核定应测次数"));
        // 核定不是整数（30.0 不算整数）
        assertTrue(PointLedgerRules.quantityErrors("30.0", "3").get(0).contains("核定应测次数"));
        assertTrue(PointLedgerRules.quantityErrors("abc", "3").get(0).contains("核定应测次数"));
        // 核定负数
        assertTrue(PointLedgerRules.quantityErrors("-1", "0").get(0).contains("核定应测次数"));
        // 已达标不是整数、负数：点已达标这一栏
        assertTrue(PointLedgerRules.quantityErrors("10", "3.5").get(0).contains("已核验达标次数"));
        assertTrue(PointLedgerRules.quantityErrors("10", "-2").get(0).contains("已核验达标次数"));
        // 已达标反超核定
        assertTrue(PointLedgerRules.quantityErrors("5", "6").get(0).contains("反超"));
        // 两栏齐、已达标空着（按 0 认）：无错
        assertTrue(PointLedgerRules.quantityErrors("10", null).isEmpty());
        assertTrue(PointLedgerRules.quantityErrors("10", "10").isEmpty());
    }

    @Test
    public void slotOnlyOpenRowsAndLegacyNullWalks() {
        // 待测、已测占名额
        assertTrue(PointLedgerRules.occupiesSlot(row(PointLedgerRules.WALK_PENDING, 0, 0)));
        assertTrue(PointLedgerRules.occupiesSlot(row(PointLedgerRules.WALK_MEASURED, 0, 0)));
        // 压卡、叫停、已删腾名额
        assertFalse(PointLedgerRules.occupiesSlot(row(PointLedgerRules.WALK_SEALED, 0, 0)));
        assertFalse(PointLedgerRules.occupiesSlot(row(PointLedgerRules.WALK_PENDING,
                PointLedgerRules.HALTED, 0)));
        assertFalse(PointLedgerRules.occupiesSlot(row(PointLedgerRules.WALK_PENDING, 0, 1)));
        // 旧纸簿行步数空着：不能当成已压卡，仍占名额
        assertTrue(PointLedgerRules.occupiesSlot(row(null, 0, 0)));
        assertFalse(PointLedgerRules.occupiesSlot(null));
    }

    @Test
    public void walkGates() {
        assertTrue(PointLedgerRules.canAdvance(PointLedgerRules.WALK_PENDING));
        assertTrue(PointLedgerRules.canAdvance(PointLedgerRules.WALK_MEASURED));
        // 压卡没有下一步
        assertFalse(PointLedgerRules.canAdvance(PointLedgerRules.WALK_SEALED));
        // 只有已测退得回；待测退无可退，压卡退不动
        assertFalse(PointLedgerRules.canRollback(PointLedgerRules.WALK_PENDING));
        assertTrue(PointLedgerRules.canRollback(PointLedgerRules.WALK_MEASURED));
        assertFalse(PointLedgerRules.canRollback(PointLedgerRules.WALK_SEALED));
        assertTrue(PointLedgerRules.isSealed(PointLedgerRules.WALK_SEALED));
        assertFalse(PointLedgerRules.isSealed(null));
    }

    @Test
    public void replayStepsCountsTrailOnly() {
        // 没有留痕：步数不明（null），不等于压卡
        assertNull(PointLedgerRules.replaySteps(Arrays.<String>asList()));
        assertNull(PointLedgerRules.replaySteps(Arrays.asList(PointLedgerRules.ACT_CREATE)));
        // 推进一下到已测
        assertEquals(Integer.valueOf(1),
                PointLedgerRules.replaySteps(Arrays.asList(
                        PointLedgerRules.ACT_CREATE, PointLedgerRules.ACT_STEP)));
        // 已测收回重来：回到待测；先前那趟仍在串上但步数从头
        assertEquals(Integer.valueOf(0),
                PointLedgerRules.replaySteps(Arrays.asList(
                        PointLedgerRules.ACT_CREATE, PointLedgerRules.ACT_STEP, PointLedgerRules.ACT_BACK)));
        // 推进两下到压卡，再推也加不上去；叫停不挪步
        assertEquals(Integer.valueOf(2),
                PointLedgerRules.replaySteps(Arrays.asList(
                        PointLedgerRules.ACT_STEP, PointLedgerRules.ACT_STEP,
                        PointLedgerRules.ACT_STEP, PointLedgerRules.ACT_HALT)));
    }

    @Test
    public void walkNamesAndZoneDrift() {
        assertEquals("待测", PointLedgerRules.walkName(0));
        assertEquals("已测", PointLedgerRules.walkName(1));
        assertEquals("压卡", PointLedgerRules.walkName(2));
        assertEquals("步数不明", PointLedgerRules.walkName(null));

        assertTrue(PointLedgerRules.zoneNameDrifts(null, "区册全称甲"));
        assertTrue(PointLedgerRules.zoneNameDrifts("旧称", "区册全称甲"));
        assertFalse(PointLedgerRules.zoneNameDrifts("区册全称甲", "区册全称甲"));
        // 底册自己没名时谈不上对不齐
        assertFalse(PointLedgerRules.zoneNameDrifts("随便", null));

        assertTrue(PointLedgerRules.operatorBlank("  "));
        assertTrue(PointLedgerRules.codeBlank(""));
        assertTrue(PointLedgerRules.zoneBlank(null));
    }
}
