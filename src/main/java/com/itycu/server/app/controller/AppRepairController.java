package com.itycu.server.app.controller;


import com.itycu.server.app.dto.baoxiu.RepairInsertDataDTO;
import com.itycu.server.app.dto.baoxiu.RepairZcInfoListDTO;
import com.itycu.server.app.dto.baoxiu.RepairZcItemRecordListDTO;
import com.itycu.server.app.util.FailMap;
import com.itycu.server.app.vo.baoxiu.RepairZcInfoListVO;
import com.itycu.server.app.vo.baoxiu.RepairZcInfoRecordVO;
import com.itycu.server.app.vo.baoxiu.ZcRepairItemVO;
import com.itycu.server.dao.RepairsDao;
import com.itycu.server.dao.ZcRepairDao;
import com.itycu.server.dao.ZcRepairItemDao;
import com.itycu.server.dto.ZcRepairDto;
import com.itycu.server.dto.ZcRepairItemDto;
import com.itycu.server.model.SysUser;
import com.itycu.server.model.ZcRepairItem;
import com.itycu.server.service.RepairService;
import com.itycu.server.service.ZcRepairService;
import com.itycu.server.utils.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Api(tags = "app资产报修接口")
@RestController
@RequestMapping(value = "/app/repair")
public class AppRepairController {


    private static Logger logger = LoggerFactory.getLogger(AppRepairController.class);

    @Autowired
    private RepairsDao repairDao;

    @Autowired
    private ZcRepairDao zcRepairDao;

    @Autowired
    private ZcRepairService zcRepairService;

    @Autowired
    private ZcRepairItemDao zcRepairItemDao;


    @PostMapping(value = "/list")
    @ApiOperation(value = "获取报修的资产列表", notes = "获取报修的资产列表")
    public Map<String, Object> getRepairVOList(@RequestBody RepairZcInfoListDTO repairZcInfoListDTO) {
        Map<String, Object> map = new HashMap<>();
        try {
            int page = repairZcInfoListDTO.getOffset();
            int limit = repairZcInfoListDTO.getLimit();
            Map<String, Object> params = new HashMap<>();
            SysUser sysUser = UserUtil.getLoginUser();
            params.put("id", sysUser.getDeptid());
            params.put("keyword", repairZcInfoListDTO.getKeyword());
            List<RepairZcInfoListVO> listVOS = repairDao.getRepairVOList(params, page * limit - limit, limit);
            map.put("data", listVOS);
            map.put("code", 0);
            map.put("message", "成功");

        } catch (Exception e) {
            logger.info("获取报修的资产列表失败{}", e.getMessage());
            return FailMap.createFailMapMsg("获取报修的资产列表失败");
        }
        return map;
    }


    @PostMapping(value = "/repair/recordList")
    @ApiOperation(value = "获取报修的【记录列表】", notes = "获取报修的【记录列表】")
    public Map<String, Object> getRecordList(@RequestBody RepairZcInfoListDTO repairZcInfoListDTO) {
        Map<String, Object> map = new HashMap<>();
        try {
            List<RepairZcInfoRecordVO> listVO = new ArrayList<>();
            int page = repairZcInfoListDTO.getOffset();
            int limit = repairZcInfoListDTO.getLimit();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("del", "0");
            List<ZcRepairDto> list = zcRepairDao.list(params, page * limit - limit, limit);
            if (!CollectionUtils.isEmpty(list)) {
                list.forEach(k -> {
                    RepairZcInfoRecordVO recordVO = new RepairZcInfoRecordVO();
                    recordVO.setId(k.getId().intValue());
                    recordVO.setDeptname(k.getDeptname());
                    recordVO.setNickname(k.getNickname());
                    recordVO.setCode(k.getCode());
                    recordVO.setCreateTime(k.getCreateTime());
                    listVO.add(recordVO);
                });
            }
            map.put("data", listVO);
            map.put("code", "0");
            map.put("msg", "成功");
        } catch (Exception e) {
            logger.info("获取报修的【记录列表】==>{}", e.getMessage());
            return FailMap.createFailMapMsg("获取报修的记录列表失败");
        }
        return map;
    }


//    @PostMapping(value = "/repair/record/zcList")
//    @ApiOperation(value = "获取报修记录的【资产列表】", notes = "获取报修记录的【资产列表】")
//    public Map<String, Object> getRepairRecordItemList(@RequestBody RepairZcInfoListDTO repairZcInfoListDTO) {
//        Map<String, Object> map = new HashMap<>();
//        try {
//
//            int page = repairZcInfoListDTO.getOffset();
//            int limit = repairZcInfoListDTO.getLimit();
//            Map<String, Object> params = new HashMap<String, Object>();
//            params.put("del", "0");
//            List<ZcRepairDto> list = zcRepairDao.list(params, page * limit - limit, limit);
//            map.put("data", list);
//            map.put("code", "0");
//            map.put("msg", "成功");
//        } catch (Exception e) {
//            logger.info("获取报修的【记录列表】==>{}", e.getMessage());
//            return FailMap.createFailMapMsg("获取报修的记录列表失败");
//        }
//        return map;
//    }


    @PostMapping(value = "/listByZcReId")
    @ApiOperation(value = "根据报修记录资产数据列表", notes = "根据报修记录资产数据列表")
    public Map<String, Object> listByZcReId(@RequestBody RepairZcItemRecordListDTO repairZcItemRecordListDTO) {
        Map<String, Object> map = new HashMap<>();
        try {

            List<ZcRepairItemVO> itemVOS = new ArrayList<>();
            List<ZcRepairItemDto> list = new ArrayList<>();
            if (repairZcItemRecordListDTO != null) {
                list = zcRepairItemDao.listByZcReId(repairZcItemRecordListDTO.getId());
                if (!CollectionUtils.isEmpty(list)) {
                    list.forEach(k -> {
                        ZcRepairItemVO repairItemVO = new ZcRepairItemVO();
                        BeanUtils.copyProperties(k, repairItemVO);
                        itemVOS.add(repairItemVO);
                    });
                }
            }
            map.put("data", itemVOS);
            map.put("code", "0");
            map.put("msg", "成功");
        } catch (Exception e) {
            logger.info("根据报修记录资产数据列表==>{}", e.getMessage());
            return FailMap.createFailMapMsg("根据报修记录资产数据列表失败");
        }
        return map;
    }


    @PostMapping(value = "/insertRepairData")
    @ApiOperation(value = "提交上传的报修的信息", notes = "提交上传的报修的信息")
    public Map<String, Object> insertRepairData(@RequestBody List<RepairInsertDataDTO> repairInsertDataDTOS) {
        Map<String, Object> map = new HashMap<>();
        try {
            if (CollectionUtils.isEmpty(repairInsertDataDTOS)) {
                return FailMap.createFailMapMsg("报修数据为空");
            }
            List<ZcRepairItem> zcRepairItemList = new ArrayList<>();
            for (RepairInsertDataDTO zcItemRecordListDTO : repairInsertDataDTOS) {
                ZcRepairItem zcRepairItem = new ZcRepairItem();
                zcRepairItem.setGlDeptId(zcItemRecordListDTO.getGlDeptId());
                zcRepairItem.setZcId(zcItemRecordListDTO.getId());
                zcRepairItem.setApplyDeptId(zcItemRecordListDTO.getSyDeptId());
                zcRepairItem.setFrontPicsUrl(zcItemRecordListDTO.getImageUrl());
                zcRepairItem.setId(zcItemRecordListDTO.getId());
                zcRepairItem.setDel(0);
                zcRepairItem.setCreateBy(UserUtil.getLoginUser().getId());
                zcRepairItemList.add(zcRepairItem);
            }
            ZcRepairDto zcRepairDto = new ZcRepairDto();
            zcRepairDto.setType("1");
            zcRepairDto.setZcRepairItemList(zcRepairItemList);
            zcRepairService.save(zcRepairDto);
            map.put("data", null);
            map.put("code", "0");
            map.put("msg", "成功");
        } catch (Exception e) {
            logger.info("提交上传的报修的信息==>{}", e.getMessage());
            return FailMap.createFailMapMsg("提交上传的报修的信息失败");
        }
        return map;
    }

}
