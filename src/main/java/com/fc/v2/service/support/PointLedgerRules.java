package com.fc.v2.service.support;

import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TNslpPoint;

/**
 * 点位建档与在走纯口径（不碰库、不沾 Spring）。
 *
 * 建行过五道筛，次序钉死不许颠倒，前一道没过不进后一道；每道筛各报各的错，
 * 谁也不许并成一句「数据不对」：
 * <ol>
 *   <li>点位码：随材料进来，不另发、不核构成规矩；同年同册同码且仍在册的行
 *       已在库里，建行拒掉；操作对象正是那一行自己时按更新办。</li>
 *   <li>挂靠：代号在区册底里查无此册，建行建不成，错要落在代号这一栏；
 *       已撤出的段不再收新行；行上冗余全称对不齐时以区册为准改齐。</li>
 *   <li>数量：核定应测、已达标两栏逐栏点名；还差几回＝核定应测－已达标，
 *       每回写库之前重轧，重算结果盖掉上一回落库值。</li>
 *   <li>名额：同一处点位同时只容一行在走；没压卡、没叫停之前第二行开不出来。</li>
 *   <li>落笔：谁建的册、谁改的行都得点得出人；两处都指不出人的按没办完处理。</li>
 * </ol>
 *
 * 建档岗一条一条建与复测业务核收后带出走同一套口径，界面不长第二种样子；
 * 早年纸簿搬进来的旧行走同一套算法，不因建档迟早分岔。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class PointLedgerRules {

    /** 在走三态：待测 */
    public static final int WALK_PENDING = 0;
    /** 在走三态：已测 */
    public static final int WALK_MEASURED = 1;
    /** 在走三态：压卡（走到头，添不进、动不得、撤不掉） */
    public static final int WALK_SEALED = 2;
    /** 在走三态共三档，步子不许乱踏 */
    public static final int WALK_STEP_COUNT = 3;

    /** 就地叫停：在走 */
    public static final int NOT_HALTED = 0;
    /** 就地叫停：已叫停（腾出同点位名额） */
    public static final int HALTED = 1;

    /** 来路：建档岗一条一条建 */
    public static final String SRC_MANUAL = "建档岗建";
    /** 来路：复测业务核收之后带出来 */
    public static final String SRC_RETEST = "复测核收带出";

    /** 留痕动作：建行 */
    public static final String ACT_CREATE = "建行";
    /** 留痕动作：推进 */
    public static final String ACT_STEP = "推进";
    /** 留痕动作：收回重来 */
    public static final String ACT_BACK = "收回";
    /** 留痕动作：就地叫停 */
    public static final String ACT_HALT = "叫停";
    /** 留痕动作：压卡 */
    public static final String ACT_SEAL = "压卡";

    /** 三档名字，档数与名字只此一处出处 */
    private static final String[] WALK_NAMES = {"待测", "已测", "压卡"};

    private PointLedgerRules() {
    }

    /** 步数回话：0..2 出本名，其余（含旧行留空）只有「步数不明」，不替它猜 */
    public static String walkName(Integer step) {
        if (step != null && step >= 0 && step < WALK_STEP_COUNT) {
            return WALK_NAMES[step];
        }
        return "步数不明";
    }

    // ------------------------------------------------------------------
    // 第一道筛：点位码
    // ------------------------------------------------------------------

    /** 点位码空着（随件该来没来）；构成规矩一律不核 */
    public static boolean codeBlank(String pointNo) {
        return pointNo == null || pointNo.trim().isEmpty();
    }

    // ------------------------------------------------------------------
    // 第二道筛：挂靠
    // ------------------------------------------------------------------

    /** 区册代号空着 */
    public static boolean zoneBlank(String zoneNo) {
        return zoneNo == null || zoneNo.trim().isEmpty();
    }

    /**
     * 行上冗余的区册全称跟区册底对不齐（含行上没抄全称）：以区册为准，
     * 把行上这一格改过来；两处各留一份说法的，算缺陷，由落库处记一笔。
     */
    public static boolean zoneNameDrifts(String rowZoneName, String bookZoneName) {
        if (bookZoneName == null) {
            return false;
        }
        return rowZoneName == null || !bookZoneName.equals(rowZoneName);
    }

    // ------------------------------------------------------------------
    // 第三道筛：数量（唯一轧算口：30 配 12 得 18，20 配 7 得 13，两数一样得 0）
    // ------------------------------------------------------------------

    /**
     * 两栏逐栏点名，次序固定：核定应测没填 → 核定应测不是整数 → 核定应测是负数
     * → 已达标不是整数 → 已达标是负数 → 已达标反超核定。
     * 一样不沾回空单；沾了回那句点名的话（指明是哪一栏不合），多句用「；」连上。
     * 已达标栏空着按 0 认（刚建行还没核验过）；非整数两栏都点。
     */
    public static List<String> quantityErrors(String needRaw, String gotRaw) {
        List<String> errors = new ArrayList<String>();
        boolean needBlank = needRaw == null || needRaw.trim().isEmpty();
        boolean gotBlank = gotRaw == null || gotRaw.trim().isEmpty();
        if (needBlank) {
            errors.add("核定应测次数这一栏没填，这一行进不了库");
        }
        Integer need = null;
        Integer got = null;
        if (!needBlank) {
            need = parseOrNull(needRaw);
            if (need == null) {
                errors.add("核定应测次数这一栏得是整数，报上来的「" + needRaw.trim() + "」不是整数");
            } else if (need.intValue() < 0) {
                errors.add("核定应测次数这一栏不能是负数（报上来 " + need + "）");
            }
        }
        if (!gotBlank) {
            got = parseOrNull(gotRaw);
            if (got == null) {
                errors.add("已核验达标次数这一栏得是整数，报上来的「" + gotRaw.trim() + "」不是整数");
            } else if (got.intValue() < 0) {
                errors.add("已核验达标次数这一栏不能是负数（报上来 " + got + "）");
            }
        }
        if (need != null && got != null && got.intValue() > need.intValue()) {
            errors.add("已核验达标次数（" + got + "）反超核定应测次数（" + need + "），这一行进不了库");
        }
        return errors;
    }

    /** 空串或非整数回 null；只认整数，30.0 这类不算整数 */
    public static Integer parseOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty() || !s.matches("-?\\d+")) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 还差几回（唯一轧算口）：核定应测－已达标。调用前两栏须已过第三道筛。
     * 每回写库之前都拿这一处重轧的结果盖掉上一回落库的那一个。
     */
    public static int lack(int need, int got) {
        return need - got;
    }

    // ------------------------------------------------------------------
    // 第四道筛：同点位在走名额
    // ------------------------------------------------------------------

    /**
     * 这一行占不占「在走」名额：没压卡、也没叫停才占。
     * 旧纸簿搬进来的行 walk_status 留空——留空不等于压卡，在走的旧行照旧占名额，
     * 绝不把在走的旧行当成已经压了卡把名额腾出去。
     */
    public static boolean occupiesSlot(TNslpPoint row) {
        if (row == null) {
            return false;
        }
        if (row.getDelFlag() != null && row.getDelFlag().intValue() == 1) {
            return false;
        }
        if (row.getHalted() != null && row.getHalted().intValue() == HALTED) {
            return false;
        }
        Integer walk = row.getWalkStatus();
        return walk == null || walk.intValue() < WALK_SEALED;
    }

    // ------------------------------------------------------------------
    // 第五道筛：落笔人
    // ------------------------------------------------------------------

    /** 建行/改行都得点得出落笔的是哪一位；指不出人的按没办完处理 */
    public static boolean operatorBlank(String operator) {
        return operator == null || operator.trim().isEmpty();
    }

    // ------------------------------------------------------------------
    // 在走步子：当前步数由系统依已走步数自行数出
    // ------------------------------------------------------------------

    /** 只认紧邻的下一步：待测→已测→压卡，乱踏不生效 */
    public static boolean canAdvance(int from) {
        return from >= WALK_PENDING && from < WALK_SEALED;
    }

    /** 还没到压卡，已测可以退回一步重来；待测退无可退，压卡退不动 */
    public static boolean canRollback(int from) {
        return from == WALK_MEASURED;
    }

    /** 压卡即到头：往后添不进去、字动不得、整行撤不掉 */
    public static boolean isSealed(Integer walkStatus) {
        return walkStatus != null && walkStatus.intValue() == WALK_SEALED;
    }

    /**
     * 依留痕把已走步数数出来：一笔「推进」加一步，一笔「收回」减一步，
     * 「压卡」钉在末档，「叫停」不挪步。数出来的结果夹在 [0,2]。
     * 旧行一笔留痕都没有时回 null（步数不明，不等于压卡）。
     */
    public static Integer replaySteps(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        int step = 0;
        boolean everMoved = false;
        for (String a : actions) {
            if (ACT_STEP.equals(a)) {
                if (step < WALK_SEALED) {
                    step++;
                }
                everMoved = true;
            } else if (ACT_BACK.equals(a)) {
                if (canRollback(step)) {
                    step--;
                }
                everMoved = true;
            } else if (ACT_SEAL.equals(a)) {
                step = WALK_SEALED;
                everMoved = true;
            }
        }
        return everMoved ? Integer.valueOf(step) : null;
    }

    /** 把多条点名消息用「；」连成一句抛到建档岗眼前，仍保持一条一条点得出 */
    public static String join(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        for (String m : messages) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(m);
        }
        return sb.toString();
    }
}
