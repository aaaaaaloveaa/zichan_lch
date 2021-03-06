package com.itycu.server.app.controller;

import com.itycu.server.app.dto.goumai.BuyItemDetailDTO;
import com.itycu.server.app.dto.goumai.InsertBuyDataDTO;
import com.itycu.server.app.util.FailMap;
import com.itycu.server.app.vo.fenye.PageVO;
import com.itycu.server.dao.ZcBuyDao;
import com.itycu.server.dao.ZcBuyItemDao;
import com.itycu.server.dto.ZcBuyDto;
import com.itycu.server.dto.ZcBuyCheckDto;
import com.itycu.server.model.SysUser;
import com.itycu.server.service.ZcBuyService;
import com.itycu.server.utils.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping(value = "/buy")
@RestController
@Api(tags = "App购买申请")
public class AppBuyController {

    private static Logger logger = LoggerFactory.getLogger(AppBuyController.class);

    @Autowired
    ZcBuyService zcBuyService;

    @Autowired
    ZcBuyDao zcBuyDao;

    @Autowired
    ZcBuyItemDao zcBuyItemDao;


    @PostMapping(value = "/insertData")
    @ApiOperation(value = "增加购买的信息", notes = "增加购买的信息")
    public Map<String, Object> insertBuyData(@RequestBody InsertBuyDataDTO insertBuyDataDTO) {
        try {
            Map<String, Object> map = new HashMap<>();
            ZcBuyDto zcBuyDto = new ZcBuyDto();
            BeanUtils.copyProperties(insertBuyDataDTO, zcBuyDto);
            zcBuyDto.setType("1");
            zcBuyService.save(zcBuyDto);
            map.put("code", 0);
            map.put("message", "成功");
            map.put("data", null);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("增加购买的信息错误{}", e.getMessage());
            return FailMap.createFailMapMsg("");
        }
    }


    @PostMapping(value = "/buyRecordList")
    @ApiOperation(value = "购买的记录列表", notes = "购买的记录列表")
    public Map<String, Object> getBuyRecordList(@RequestBody PageVO pageVO) {
        try {
            Map<String, Object> map = new HashMap<>();
            Map<String, Object> params = new HashMap<>();
            SysUser sysUser = UserUtil.getLoginUser();
            params.put("glDeptId", sysUser.getDeptid());
            if ("cwb".equals(sysUser.getC03())) {
                params.put("type", "cw");
            } else {
                params.put("type", "gl");
            }
            Integer page = pageVO.getOffset();
            Integer limit = pageVO.getLimit();
            List<Map<String, Object>> list = zcBuyDao.listZcBuy(params, page * limit - limit, limit);
            map.put("data", list);
            map.put("code", "0");
            map.put("msg", "查询成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("增加购买的信息错误{}", e.getMessage());
            return FailMap.createFailMapMsg("");
        }
    }


    @PostMapping(value = "/getBuyRecordItemDetailList")
    @ApiOperation(value = "购买的记录列表下面的详情数据列表", notes = "购买的记录列表下面的详情数据列表")
    public Map<String, Object> getBuyRecordItemDetailList(@RequestBody BuyItemDetailDTO buyItemDetailDTO) {
        try {
            Map<String, Object> map = new HashMap<>();
            List<Map<String, Object>> list = new ArrayList<>();
            if (null != buyItemDetailDTO.getZcBuyId()) {
                list = zcBuyItemDao.listDetailByZcBfId(buyItemDetailDTO.getZcBuyId(), buyItemDetailDTO.getCw());
            }
            map.put("data", list);
            map.put("code", "0");
            map.put("msg", "查询成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("增加购买的信息错误{}", e.getMessage());
            return FailMap.createFailMapMsg("");
        }
    }

    @PostMapping("/buyCheckMainInfo")
    @ApiOperation(value = "资产购买审核主信息",notes = "资产购买审核主信息")
    public Map buyCheckMainInfo(@RequestBody ZcBuyCheckDto zcBuyCheckDto) {
        Map<String,Object> map = new HashMap();
        try {
            HashMap<String,Object> data = zcBuyDao.getZcBuyDetail(zcBuyCheckDto.getZcBuyId());
            map.put("data",data);
            map.put("code","0");
            map.put("message","成功");
        } catch (Exception e) {
            logger.info("资产购买审核主信息{}", e.getMessage());
            return FailMap.createFailMap();
        }
        return map;
    }

    @PostMapping("/buyCheckItemList")
    @ApiOperation(value = "资产购买审核列表信息",notes = "资产购买审核列表信息")
    public Map buyCheckItemList(@RequestBody ZcBuyCheckDto zcBuyCheckDto) {
        Map<String,Object> map = new HashMap();
        try {
            List<Map<String,Object>> list = zcBuyItemDao.listDetailByFlowTodoIdNew(zcBuyCheckDto.getTodoId());
            map.put("data",list);
            map.put("code","0");
            map.put("message","成功");
        } catch (Exception e) {
            logger.info("资产购买审核列表信息{}", e.getMessage());
            return FailMap.createFailMap();
        }
        return map;
    }

    @PostMapping("/buyCheck")
    @ApiOperation(value = "资产购买审核",notes = "资产购买审核")
    public Map buyCheck(@RequestBody ZcBuyCheckDto zcBuyCheckDto) {
        Map<String,Object> map = new HashMap();
        try {
            String itemStatus = zcBuyService.check(zcBuyCheckDto);
            HashMap<String, Object> data = new HashMap<>();
            data.put("status",itemStatus);
            map.put("data",data);
            map.put("code","0");
            map.put("message","请求成功");
        } catch (Exception e) {
            logger.info("资产购买审核{}", e.getMessage());
            return FailMap.createFailMap();
        }
        return map;
    }

}
