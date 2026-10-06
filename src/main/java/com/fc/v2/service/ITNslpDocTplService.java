package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TNslpDocIssue;
import com.fc.v2.model.auto.TNslpDocTpl;
import com.fc.v2.model.custom.DocIssueRequest;

/**
 * 执法文书模板版次与出文 Service接口。
 *
 * 三个文种各存若干版，版次数值大的先开口；本轮照哪一版取值只认出文那一刻
 * （起用头含、交班尾不含、情形在位）。按版次取值与整表取优两个位置同刻问
 * 同一行文种，必给同一版。已撤版不进现行可选清单，照主键仍查得着。
 * 「还差几项」由应填项数减已取项数轧出，不留手填格子。旧文照当初那版留在
 * 档上，新立版不回改旧文。
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface ITNslpDocTplService {

    /** 照主键点版（已撤版也点得到；不进现行清单不等于从库里抹掉） */
    TNslpDocTpl selectById(Long id);

    /** 现行可选清单：at 那一刻在位的版（已撤版不进），按文种、版次排列 */
    List<TNslpDocTpl> listActive(Date at);

    /** 现行清单按文种过滤（已撤版不进） */
    List<TNslpDocTpl> listActiveByName(String tplName, Date at);

    /**
     * 按版次取值：给文种与出文那一刻，回该开口的那一版（唯一选版口）。
     * 与 {@link #resolveTop} 同刻同文种必给同一版；这一刻无在线版回 null。
     */
    TNslpDocTpl resolveVersion(String tplName, Date at);

    /**
     * 整表取优：全表在线诸版里给该文种开口的那一版。内里与
     * {@link #resolveVersion} 走同一个选版口，两答必同版。
     */
    TNslpDocTpl resolveTop(String tplName, Date at);

    /**
     * 立版／改版：两栏逐栏点名（没填、负数、已取反超应填），齐了落库，
     * 「还差几项」由唯一口径重轧后盖掉上一回落库值。
     */
    TNslpDocTpl saveVersion(TNslpDocTpl tpl, String operator);

    /** 立版／改版（落笔人取当前登录岗） */
    TNslpDocTpl saveVersion(TNslpDocTpl tpl);

    /** 撤版：不进现行可选清单，但照主键仍查得着；返回更新行数 */
    int withdrawVersion(Long id, String operator);

    /** 撤版（落笔人取当前登录岗） */
    int withdrawVersion(Long id);

    /**
     * 出文：选版只认服务端这一处；应填项一项项从点位行取值，缺一项一项点名，
     * 缺着就出不来，不许把空白纸发去打印。齐了落一纸出文档案，
     * 把出文那一刻照的版次与三项数钉成快照（往后新版不回改这一纸）。
     */
    TNslpDocIssue issue(DocIssueRequest request, String operator);

    /** 出文（落笔人取当前登录岗） */
    TNslpDocIssue issue(DocIssueRequest request);

    /** 出文档案：某一行点位历次出的文（旧文照当初版次留档） */
    List<TNslpDocIssue> listIssues(Long pointId);

    /** 照主键点一纸出文档案 */
    TNslpDocIssue selectIssueById(Long id);
}
