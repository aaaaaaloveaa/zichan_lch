package com.itycu.server.app.controller;

import com.itycu.server.app.dto.ZcInfoListDTO;
import com.itycu.server.app.dto.index.ZcInfoEpcIdDTO;
import com.itycu.server.app.model.AppIndexDeptDataInfo;
import com.itycu.server.app.model.AppIndexZcValueAndNumber;
import com.itycu.server.app.service.IndexService;
import com.itycu.server.app.util.FailMap;
import com.itycu.server.app.vo.index.IndexZcInfoVO;
import com.itycu.server.app.vo.todovo.AppTodoVO;
import com.itycu.server.app.vo.zonghang.ZongHangMonthNumber;
import com.itycu.server.dto.AppDealStatus;
import com.itycu.server.dto.ZcInfoDto;
import com.itycu.server.model.SysUser;
import com.itycu.server.service.ZcInfoService;
import com.itycu.server.utils.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "app首页接口")
@RequestMapping(value = "/api")
public class AppIndexController {


    private static Logger logger = LoggerFactory.getLogger(AppIndexController.class);

    @Autowired
    ZcInfoService zcInfoService;

    @Autowired
    IndexService indexService;

    @ApiOperation(value = "获取资产净值和资产数量", notes = "获取资产净值和资产数量")
    @PostMapping(value = "/getZcValueAndZcNumber")
    public Map<String, Object> getZcValueAndZcNumber() {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            if (null != sysUser) {
                AppIndexZcValueAndNumber zcValueAndNumber = zcInfoService.getZcValueAndZcNumber(sysUser);
                map.put("code", 0);
                map.put("message", "成功");
                map.put("data", zcValueAndNumber);
            }
        } catch (Exception e) {
            logger.error("获取资产净值和资产数量错误,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }

    @ApiOperation(value = "总行获取资产统计和支行统计", notes = "总行获取资产统计和支行统计")
    @PostMapping(value = "/getZongHangZcNumber")
    public Map<String, Object> getZongHangZcNumber() {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            if (null != sysUser && "664000".equals(sysUser.getUsername())) {
                ZongHangMonthNumber zongHangMonthNumber = zcInfoService.getZongHangZcNumber(sysUser);

                map.put("code", 0);
                map.put("message", "请求成功");
                map.put("data", zongHangMonthNumber);
            }else {
                map.put("code", 500);
                map.put("message", "你不具备查询条件");
                map.put("data", null);
            }
        } catch (Exception e) {
            logger.error("总行获取资产统计和支行统计,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }


    @ApiOperation(value = "获取所有的管理部门", notes = "获取所有的管理部门")
    @PostMapping(value = "/getAllManagerDeptList")
    public Map<String, Object> getAllManagerDeptList() {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            List<AppIndexDeptDataInfo> appIndexDeptDataInfoList = indexService.getAllManagerDeptList(sysUser.getDeptid());
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", appIndexDeptDataInfoList);
        } catch (Exception e) {
            logger.error("获取所有的管理部门,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }


    @ApiOperation(value = "获取所有的部门和支行", notes = "获取所有的部门和支行")
    @PostMapping(value = "/getAllBranchDeptList")
    public Map<String, Object> getAllBranchDeptList() {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            List<AppIndexDeptDataInfo> appIndexDeptDataInfoList = indexService.getAllBranchDeptList(sysUser.getDeptid());
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", appIndexDeptDataInfoList);
        } catch (Exception e) {
            logger.error("获取所有的部门和支行,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }


    @ApiOperation(value = "获取所有的资产", notes = "获取所有的资产")
    @PostMapping(value = "/getAllZcList")
    public Map<String, Object> getAllZcList(@RequestBody ZcInfoListDTO zcInfoListDTO) {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            List<ZcInfoDto> list = zcInfoService.getAllZcInfoListByUser(sysUser, zcInfoListDTO);
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", list);
        } catch (Exception e) {
            logger.error("获取所有的资产,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }


    @ApiOperation(value = "待办事项列表", notes = "待办事项列表")
    @PostMapping(value = "/getAllWailDealList")
    public Map<String, Object> getAllWailDealList() {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            List<AppTodoVO> list = indexService.appQueryAllTodoList(sysUser.getId());
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", list);
        } catch (Exception e) {
            logger.error("待办事项列表,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }

    @ApiOperation(value = "待办事项列表", notes = "待办事项列表")
    @PostMapping(value = "/getAppAllWailDealList")
    public Map<String, Object> getAppAllWailDealList(@RequestBody AppDealStatus appDealStatus) {
        Map<String, Object> map = new HashMap<>();
        SysUser sysUser = UserUtil.getLoginUser();
        try {
            List<AppTodoVO> list = indexService.appQueryAllTodoList(sysUser.getId(),appDealStatus.getStatus());
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", list);
        } catch (Exception e) {
            logger.error("待办事项列表,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }


    @ApiOperation(value = "首页扫一扫获取资产信息数据", notes = "首页扫一扫获取资产信息数据")
    @PostMapping(value = "/getZcInfo")
    public Map<String, Object> getZcInfo(@RequestBody ZcInfoEpcIdDTO zcInfoEpcIdDTO) {
        Map<String, Object> map = new HashMap<>();
        try {
            IndexZcInfoVO zcInfoVO = null;
            ZcInfoDto zcInfoDto = zcInfoService.queryZnInfoDtoByEpcId(zcInfoEpcIdDTO.getEpcid());
            if (null != zcInfoDto) {
                zcInfoVO = new IndexZcInfoVO();
                BeanUtils.copyProperties(zcInfoVO, zcInfoDto);
            }
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", zcInfoVO);
        } catch (Exception e) {
            logger.error("待办事项列表,{}", e.getMessage());
            map = FailMap.createFailMap();
        }
        return map;
    }


}
