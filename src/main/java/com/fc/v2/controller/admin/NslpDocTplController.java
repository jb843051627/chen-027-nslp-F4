package com.fc.v2.controller.admin;

import java.util.Date;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TNslpDocTpl;
import com.fc.v2.model.custom.DocIssueRequest;
import com.fc.v2.service.ITNslpDocTplService;
import com.fc.v2.util.DateUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 执法文书模板版次与出文 Controller。
 *
 * 出文走打印环节，取值只认版次：本轮照哪一版由服务端按出文那一刻唯一选出，
 * 页面不许再算第二遍；同一刻按版次取值与整表取优两答同版。应填缺项一项项点名，
 * 不许发空白纸。旧文照当初版次留档，新版不回改旧文。
 *
 * @author fuce
 * @date 2026-10-06
 */
@Api(value = "执法文书模板版次与出文")
@Controller
@RequestMapping("/NslpDocTplController")
public class NslpDocTplController extends BaseController {

    private final String prefix = "admin/nslpDocTpl";

    @Autowired
    private ITNslpDocTplService nslpDocTplService;

    @ApiOperation(value = "版次管理跳转", notes = "版次管理跳转")
    @GetMapping("/view")
    @RequiresPermissions("nslp:nslpDocTpl:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "文书现行可选清单", action = "listActive")
    @ApiOperation(value = "现行可选清单", notes = "at 那一刻在位的版；已撤版不进，但照主键仍查得着")
    @GetMapping("/listActive")
    @RequiresPermissions("nslp:nslpDocTpl:list")
    @ResponseBody
    public AjaxResult listActive(String tplName, String at) {
        Date moment = DateUtils.parseDate(at);
        if (tplName != null && !tplName.trim().isEmpty()) {
            return AjaxResult.successData(200,
                    nslpDocTplService.listActiveByName(tplName.trim(), moment));
        }
        return AjaxResult.successData(200, nslpDocTplService.listActive(moment));
    }

    @ApiOperation(value = "照主键点版", notes = "已撤版也点得到")
    @GetMapping("/get")
    @RequiresPermissions("nslp:nslpDocTpl:list")
    @ResponseBody
    public AjaxResult get(Long id) {
        return AjaxResult.successData(200, nslpDocTplService.selectById(id));
    }

    @Log(title = "按版次取值", action = "resolveVersion")
    @ApiOperation(value = "按版次取值", notes = "出文那一刻在线诸版中版次大者先开口；与整表取优同答")
    @GetMapping("/resolveVersion")
    @RequiresPermissions("nslp:nslpDocTpl:list")
    @ResponseBody
    public AjaxResult resolveVersion(String tplName, String at) {
        return AjaxResult.successData(200,
                nslpDocTplService.resolveVersion(tplName, DateUtils.parseDate(at)));
    }

    @Log(title = "整表取优选版", action = "resolveTop")
    @ApiOperation(value = "整表取优", notes = "与按版次取值走同一选版口，两答必同版")
    @GetMapping("/resolveTop")
    @RequiresPermissions("nslp:nslpDocTpl:list")
    @ResponseBody
    public AjaxResult resolveTop(String tplName, String at) {
        return AjaxResult.successData(200,
                nslpDocTplService.resolveTop(tplName, DateUtils.parseDate(at)));
    }

    @Log(title = "立版/改版", action = "saveVersion")
    @ApiOperation(value = "立版/改版", notes = "三项数逐栏点名；还差几项由应填减已取轧出，不留手填格子")
    @PostMapping("/saveVersion")
    @RequiresPermissions("nslp:nslpDocTpl:add")
    @ResponseBody
    public AjaxResult saveVersion(TNslpDocTpl tpl) {
        return AjaxResult.successData(200, nslpDocTplService.saveVersion(tpl));
    }

    @Log(title = "撤版", action = "withdrawVersion")
    @ApiOperation(value = "撤版", notes = "不进现行清单，主键仍查得着，已出旧文不动")
    @PostMapping("/withdrawVersion")
    @RequiresPermissions("nslp:nslpDocTpl:add")
    @ResponseBody
    public AjaxResult withdrawVersion(Long id) {
        return toAjax(nslpDocTplService.withdrawVersion(id));
    }

    @Log(title = "出文", action = "issue")
    @ApiOperation(value = "出文", notes = "选版只认服务端；应填项缺一项一项点名；齐了落版次快照档案")
    @PostMapping("/issue")
    @RequiresPermissions("nslp:nslpDocTpl:issue")
    @ResponseBody
    public AjaxResult issue(DocIssueRequest request) {
        return AjaxResult.successData(200, nslpDocTplService.issue(request));
    }

    @ApiOperation(value = "出文档案", notes = "某行点位历次出的文，照当初版次留档")
    @GetMapping("/listIssues")
    @RequiresPermissions("nslp:nslpDocTpl:list")
    @ResponseBody
    public AjaxResult listIssues(Long pointId) {
        return AjaxResult.successData(200, nslpDocTplService.listIssues(pointId));
    }
}
