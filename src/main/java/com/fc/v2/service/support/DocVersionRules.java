package com.fc.v2.service.support;

import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TNslpDocTpl;
import com.fc.v2.model.auto.TNslpPoint;

/**
 * 执法文书版次与出文纯口径（不碰库、不沾 Spring）。
 *
 * 三个文种（超标告知·责令限期改正·复查结论）各存若干版；这一纸本轮照哪一版取值，
 * 只认出文那一刻：起用那一一日不晚于那一刻（头含）、交班那一一日晚于那一刻
 * （尾不含，留空按一直用下去认）、情形记「在位」；多条同时在线时版次数值大的
 * 先开口。选版只此一处，按版次取值与整表取优两个入口问同一行文种、同一刻，
 * 必给同一版——页面不许再算第二遍。
 *
 * 三项数里「还差几项」不留手填格子，由应填项数减已取项数轧出。出文时应填项
 * 一项一项点名：点位题名空着、限值线没挂上……缺一项这一纸就出不来，不许把
 * 空白纸当文书发去打印。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class DocVersionRules {

    /** 版次情形：在位（进现行可选清单） */
    public static final int STATUS_ACTIVE = 0;
    /** 版次情形：已撤版（不进现行可选清单，照主键仍查得着） */
    public static final int STATUS_WITHDRAWN = 1;

    /** 文种一：超标告知 */
    public static final String NAME_NOTICE = "超标告知";
    /** 文种二：责令限期改正 */
    public static final String NAME_ORDER = "责令限期改正";
    /** 文种三：复查结论 */
    public static final String NAME_REVIEW = "复查结论";

    /**
     * 出文取值项按这个次序一项项找值：前若干项各有各的取数处，
     * 本版应填项数（var_num）点到第几项就取到第几项。
     * 「应填项里但凡有一项取不到值」点的就是这串上的名。
     */
    private static final String[] FILL_ITEMS = {"点位题名", "限值线代号"};

    private DocVersionRules() {
    }

    /** 三个文种的名，只此一处出处 */
    public static boolean isKnownName(String tplName) {
        return NAME_NOTICE.equals(tplName) || NAME_ORDER.equals(tplName)
                || NAME_REVIEW.equals(tplName);
    }

    /**
     * 出文那一刻在不在线：起用那一一日已走到（头含）、交班那一一日还没到
     * （尾不含；留空按一直用下去认）、情形记在位。
     */
    public static boolean effectiveAt(TNslpDocTpl tpl, java.util.Date at) {
        if (tpl == null || at == null || tpl.getEffStart() == null) {
            return false;
        }
        if (tpl.getStatus() == null || tpl.getStatus().intValue() != STATUS_ACTIVE) {
            return false;
        }
        if (tpl.getEffStart().compareTo(at) > 0) {
            return false;
        }
        return tpl.getEffEnd() == null || at.compareTo(tpl.getEffEnd()) < 0;
    }

    /**
     * 选版（唯一入口）：同一行文种、出文那一刻在线的诸版里，版次数值大的先开口。
     * 没有一条在线回 null。调进来的该文种版本全集由落库处一次取齐，
     * 「按版次取值」与「整表取优」都走这一处，两答必同版。
     */
    public static TNslpDocTpl pickVersion(List<TNslpDocTpl> versions, String tplName,
                                          java.util.Date at) {
        if (versions == null || tplName == null || at == null) {
            return null;
        }
        TNslpDocTpl winner = null;
        for (TNslpDocTpl tpl : versions) {
            if (tpl == null || !tplName.equals(tpl.getTplName())) {
                continue;
            }
            if (!effectiveAt(tpl, at)) {
                continue;
            }
            if (winner == null || verOf(tpl) > verOf(winner)) {
                winner = tpl;
            }
        }
        return winner;
    }

    private static int verOf(TNslpDocTpl tpl) {
        return tpl.getVerNo() == null ? Integer.MIN_VALUE : tpl.getVerNo().intValue();
    }

    // ------------------------------------------------------------------
    // 三项数：还差几项 = 应填项数 - 已取项数，不留手填格子
    // ------------------------------------------------------------------

    /**
     * 两栏逐栏体检：应填项数没填／不是非负整数；已取项数没填／不是非负整数；
     * 已取反超应填。一样不沾回空单。
     */
    public static List<String> countErrors(Integer varNum, Integer fillNum) {
        List<String> errors = new ArrayList<String>();
        if (varNum == null) {
            errors.add("本版应填项数这一栏没填");
        } else if (varNum.intValue() < 0) {
            errors.add("本版应填项数这一栏不能是负数（报上来 " + varNum + "）");
        }
        if (fillNum == null) {
            errors.add("已取到值的项数这一栏没填");
        } else if (fillNum.intValue() < 0) {
            errors.add("已取到值的项数这一栏不能是负数（报上来 " + fillNum + "）");
        }
        if (varNum != null && fillNum != null && fillNum.intValue() > varNum.intValue()) {
            errors.add("已取到值的项数（" + fillNum + "）反超本版应填项数（" + varNum + "）");
        }
        return errors;
    }

    /** 还差几项唯一轧算口：应填项数－已取项数；写库前重轧，盖掉上一回落库值 */
    public static int miss(int varNum, int fillNum) {
        return varNum - fillNum;
    }

    // ------------------------------------------------------------------
    // 出文：应填项一项项从点位行取值，缺一项一项点名
    // ------------------------------------------------------------------

    /** 这一项在点位行上取不取得到值 */
    public static boolean itemFilled(String itemName, TNslpPoint point) {
        if (point == null) {
            return false;
        }
        if ("点位题名".equals(itemName)) {
            return point.getContent() != null && !point.getContent().trim().isEmpty();
        }
        if ("限值线代号".equals(itemName)) {
            return point.getPinnedRuleCode() != null
                    && !point.getPinnedRuleCode().trim().isEmpty();
        }
        // 名目上该有、却没有取数处的项，一律算取不到，不许拿空值凑一纸
        return false;
    }

    /**
     * 照本版应填项数，一项项从点位行取值，把取不到的点名报回（按项序）。
     * 报回空单才算齐：一纸才出得来。
     */
    public static List<String> missingItems(TNslpDocTpl tpl, TNslpPoint point) {
        List<String> missing = new ArrayList<String>();
        if (tpl == null || tpl.getVarNum() == null) {
            missing.add("版次上应填项数没定，这一纸出不来");
            return missing;
        }
        int varNum = tpl.getVarNum().intValue();
        for (int i = 0; i < varNum; i++) {
            String itemName = i < FILL_ITEMS.length ? FILL_ITEMS[i] : "第" + (i + 1) + "项";
            if (!itemFilled(itemName, point)) {
                missing.add("出文缺项：「" + tpl.getTplName() + "」第" + (i + 1)
                        + "项「" + itemName + "」取不到值，这一纸出不来");
            }
        }
        return missing;
    }

    /**
     * 本版此刻在这行点位上实际取得到几项（只由服务端这一处数；页面不许再算）。
     */
    public static int countFilled(TNslpDocTpl tpl, TNslpPoint point) {
        if (tpl == null || tpl.getVarNum() == null) {
            return 0;
        }
        int filled = 0;
        int varNum = tpl.getVarNum().intValue();
        for (int i = 0; i < varNum; i++) {
            String itemName = i < FILL_ITEMS.length ? FILL_ITEMS[i] : "第" + (i + 1) + "项";
            if (itemFilled(itemName, point)) {
                filled++;
            }
        }
        return filled;
    }

    /** 多条点名消息连成一句，仍保持一项项点得出 */
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
