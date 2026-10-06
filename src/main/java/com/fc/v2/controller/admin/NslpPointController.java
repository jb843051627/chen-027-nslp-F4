package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TNslpPoint;
import com.fc.v2.model.custom.LevelQuery;
import com.fc.v2.model.custom.LimitLineVerdict;
import com.fc.v2.model.custom.PointRegister;
import com.fc.v2.model.custom.RosterQuery;
import com.fc.v2.model.custom.RosterResult;
import com.fc.v2.service.ITNslpPointService;
import com.fc.v2.util.DateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 声环境监测点位底册 Controller。
 *
 * 两桩事（点位入册、照册出文）的入册这一头：建档五条筛、在走三步、名册三摆法
 * 都只认 Service 给出的答案，本处不另算第二遍。旧的增删改查入口收什么回什么
 * 维持原状，新规矩走 register/roster 等新入口。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "声环境监测点位底册")
@Controller
@RequestMapping("/NslpPointController")
public class NslpPointController extends BaseController {

    private final String prefix = "admin/nslpPoint";

    @Autowired
    private ITNslpPointService nslpPointService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("nslp:nslpPoint:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "声环境监测点位底册集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("nslp:nslpPoint:list")
    @ResponseBody
    public ResultTable list(TNslpPoint record) {
        QueryWrapper<TNslpPoint> queryWrapper = new QueryWrapper<TNslpPoint>();
        startPage();
        com.github.pagehelper.PageInfo<TNslpPoint> page =
                new com.github.pagehelper.PageInfo<TNslpPoint>(nslpPointService.selectTNslpPointList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    // ------------------------------------------------------------------
    // 建档：五道筛唯一入口（建档岗一条一条建／复测核收带出，同一处同一套样子）
    // ------------------------------------------------------------------

    @Log(title = "点位建档（建行/改行过五道筛）", action = "register")
    @ApiOperation(value = "建行/改行", notes = "五道筛依次过、各报各的错；数量、同点名额、落笔人都在这一处认")
    @PostMapping("/register")
    @RequiresPermissions("nslp:nslpPoint:add")
    @ResponseBody
    public AjaxResult register(PointRegister register) {
        // 接材料的是区局建档岗：落笔人随登录态写在那一行上，指不出人按没办完处理
        return AjaxResult.successData(200, nslpPointService.registerPoint(register));
    }

    // ------------------------------------------------------------------
    // 在走三步：步由系统依留痕数，屏上写的、人记的不作依据
    // ------------------------------------------------------------------

    @Log(title = "点位在走推进", action = "advanceWalk")
    @ApiOperation(value = "推进紧邻的下一步", notes = "待测→已测→压卡；压卡即到头")
    @PostMapping("/advanceWalk")
    @RequiresPermissions("nslp:nslpPoint:edit")
    @ResponseBody
    public AjaxResult advanceWalk(Long id) {
        return AjaxResult.successData(200, nslpPointService.advanceWalk(id));
    }

    @Log(title = "点位已测退回重来", action = "rollbackWalk")
    @ApiOperation(value = "已测退回一步", notes = "先前那一趟留痕照旧留着，不抹、不并排")
    @PostMapping("/rollbackWalk")
    @RequiresPermissions("nslp:nslpPoint:edit")
    @ResponseBody
    public AjaxResult rollbackWalk(Long id) {
        return AjaxResult.successData(200, nslpPointService.rollbackWalk(id));
    }

    @Log(title = "点位就地叫停", action = "haltPoint")
    @ApiOperation(value = "就地叫停", notes = "腾出同点位名额，名册上照常露脸")
    @PostMapping("/halt")
    @RequiresPermissions("nslp:nslpPoint:edit")
    @ResponseBody
    public AjaxResult halt(Long id, String reason) {
        return AjaxResult.successData(200, nslpPointService.haltPoint(id, reason));
    }

    @ApiOperation(value = "当前步数", notes = "由系统依已走留痕步数自行数出")
    @GetMapping("/currentStep")
    @RequiresPermissions("nslp:nslpPoint:list")
    @ResponseBody
    public AjaxResult currentStep(Long id) {
        return AjaxResult.successData(200, nslpPointService.currentStep(id));
    }

    @Log(title = "点位撤行", action = "removePoint")
    @ApiOperation(value = "撤行", notes = "压卡的撤不掉、叫停的留在册上；要改只能另起新行")
    @DeleteMapping("/removePoint")
    @RequiresPermissions("nslp:nslpPoint:remove")
    @ResponseBody
    public AjaxResult removePoint(Long id) {
        return toAjax(nslpPointService.removePoint(id));
    }

    // ------------------------------------------------------------------
    // 摆法：分页、三搜索、区层可见范围、三种视图同一底数
    // ------------------------------------------------------------------

    @Log(title = "点位名册摆法", action = "roster")
    @ApiOperation(value = "名册摆法", notes = "页长认配置；点位码/区册代号/区层类别前缀与全等都认；三视图处数同一底数")
    @GetMapping("/roster")
    @RequiresPermissions("nslp:nslpPoint:list")
    @ResponseBody
    public AjaxResult roster(RosterQuery query,
                             @RequestParam(value = "viewerZoneNos", required = false) String viewerZoneNos) {
        // 可见范围由登录岗的区层/区划授权带进来；这里按调用方所报的范围限定。
        // 本层值守岗带 viewerLayer（只看挂靠本层的行）；区局岗带 viewerZoneNos
        // （四层都看，但限划归它的那些区册）。两处都不带=系统内部问话，不限层。
        if (viewerZoneNos != null && !viewerZoneNos.trim().isEmpty()) {
            query.setViewerZoneNos(Arrays.asList(viewerZoneNos.trim().split("\\s*,\\s*")));
        }
        RosterResult result = nslpPointService.roster(query);
        return AjaxResult.successData(200, result);
    }

    @Log(title = "点位归层并钉线", action = "pinLevel")
    @ApiOperation(value = "点位归层", notes = "走限值线唯一算法；归层钉线代号与起用日，不归层两格清空白")
    @GetMapping("/pinLevel")
    @RequiresPermissions("nslp:nslpPoint:list")
    @ResponseBody
    public AjaxResult pinLevel(Long id, BigDecimal input, String at, String objKind) {
        LimitLineVerdict v = nslpPointService.pinPointLevel(id, input, DateUtils.parseDate(at), objKind);
        return AjaxResult.successData(200, v);
    }

    @Log(title = "点位批量归层（大屏同次计算）", action = "pinLevelBatch")
    @ApiOperation(value = "批量归层", notes = "大屏昼段夜段折线/点值/后端回算同一次计算并逐行钉线")
    @PostMapping("/pinLevelBatch")
    @RequiresPermissions("nslp:nslpPoint:list")
    @ResponseBody
    public AjaxResult pinLevelBatch(@RequestBody List<LevelQuery> queries) {
        return AjaxResult.successData(200, nslpPointService.pinPointLevelBatch(queries));
    }

    @Log(title = "看板挂码孤儿点位", action = "pinnedOrphans")
    @ApiOperation(value = "挂码孤儿", notes = "挂着线代号、回到 at 那一刻却寻不到在位版本的点位一条条列出")
    @GetMapping("/pinnedOrphans")
    @RequiresPermissions("nslp:nslpPoint:list")
    @ResponseBody
    public AjaxResult pinnedOrphans(String at) {
        return AjaxResult.successData(200, nslpPointService.listPinnedOrphans(DateUtils.parseDate(at)));
    }

    // ------------------------------------------------------------------
    // 旧的增删改查入口：维持原状，不动本体
    // ------------------------------------------------------------------

    @Log(title = "声环境监测点位底册新增", action = "add")
    @ApiOperation(value = "新增（旧入口，维持原状）", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("nslp:nslpPoint:add")
    @ResponseBody
    public AjaxResult add(TNslpPoint record) {
        return toAjax(nslpPointService.insertTNslpPoint(record));
    }

    @Log(title = "声环境监测点位底册修改", action = "edit")
    @ApiOperation(value = "修改保存（旧入口，维持原状）", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("nslp:nslpPoint:edit")
    @ResponseBody
    public AjaxResult editSave(TNslpPoint record) {
        return toAjax(nslpPointService.updateTNslpPoint(record));
    }

    @Log(title = "声环境监测点位底册删除", action = "remove")
    @ApiOperation(value = "删除（旧入口，维持原状）", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("nslp:nslpPoint:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(nslpPointService.deleteTNslpPointByIds(ids));
    }
}
