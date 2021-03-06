package com.itycu.server.service.impl;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import com.itycu.server.app.dto.ZcInfoListDTO;
import com.itycu.server.app.model.AppIndexZcValueAndNumber;
import com.itycu.server.app.vo.GlDeptZcCountVO;
import com.itycu.server.app.vo.zonghang.ZhiHangNumber;
import com.itycu.server.app.vo.zonghang.ZongHangMonthNumber;
import com.itycu.server.app.vo.zonghang.ZongHangZcInfo;
import com.itycu.server.dao.*;
import com.itycu.server.dto.LoginUser;
import com.itycu.server.dto.ZcInfoDto;
import com.itycu.server.model.*;
import com.itycu.server.page.table.PageTableRequest;
import com.itycu.server.service.ZcChangeRecordService;
import com.itycu.server.service.ZcInfoService;
import com.itycu.server.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ZcInfoServiceImpl implements ZcInfoService {

    private static final Logger log = LoggerFactory.getLogger("adminLogger");
    @Autowired
    private ZcInfoDao zcInfoDao;
    @Autowired
    private ZcCategoryDao zcCategoryDao;
    @Autowired
    private DeptDao deptDao;
    @Autowired
    private DictDao dictDao;
    @Autowired
    private ZcChangeRecordService zcChangeRecordService;
    @Autowired
    private ZcEpcCodeDao zcEpcCodeDao;
    @Autowired
    private PermissionDao permissionDao;
    @Autowired
    private ZcInspectDao zcInspectDao;
    @Autowired
    private ZongHangBenYueDao zongHangBenYueDao;


    @Override
    public Map save(ZcInfo zcInfo) {
        Map map = new HashMap();
        LoginUser loginUser = UserUtil.getLoginUser();
        long deptid = loginUser.getDeptid();
        Dept dept = deptDao.getById(deptid);
        Dept pDept = deptDao.getById(dept.getPid());
        int count = zcInfoDao.countByDeptcode(pDept.getDeptcode());
        String epcId = EcpIdUtil.getStaticNum(count + (1), 5);
        zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
        zcInfo.setCreateBy(UserUtil.getLoginUser().getId());
        zcInfo.setDel(0);
        zcInfo.setBf("0");
        zcInfo.setEpcid(epcId);
        zcInfoDao.save(zcInfo);
        log.debug("新增资产档案{}", zcInfo.getCreateBy() + zcInfo.getZcName());
        map.put("data",zcInfo);
        map.put("code","0");
        map.put("msg","");
        return map;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public ZcInfo update(ZcInfo zcInfo) {

        // 查询原始的资产信息
        ZcInfoDto oldZcInfo = zcInfoDao.getById(zcInfo.getId());
        zcInfo.setUpdateBy(UserUtil.getLoginUser().getId());
        zcInfoDao.update(zcInfo);
        // 资产更新
        //ZcInfo zc = new ZcInfo();
        //BeanUtils.copyProperties(oldZcInfo,zcInfo);
        ZcInfoDto after = zcInfoDao.getById(zcInfo.getId());
        List<String> fields = CompareFileds.compareFields(oldZcInfo, after, CompareFileds.getFiledName(zcInfo));
        String changeField = String.join(",", fields);
        //保存变更记录
        zcChangeRecordService.save((ZcInfo) after, changeField);
        log.debug("编辑资产档案{}", zcInfo.getUpdateBy() + zcInfo.getZcName());

        /**
         * 插入巡检的数据记录
         */
        Integer inspectTime = zcInfo.getInspectTime();
        if(null!=inspectTime && 0!= inspectTime){
            insertInspectRecode(zcInfo);
        }
        return zcInfo;
    }


    private  void insertInspectRecode(ZcInfo zcInfo){
        /**
         * 插入的巡检记录表中
         */
        insertInspectData(zcInfo);
    }

    private int insertInspectData(ZcInfo zcInfo){
        long  id = UserUtil.getLoginUser().getId();
        ZcInspect inspect = new ZcInspect();
        inspect.setZcId(zcInfo.getId());
        inspect.setDays(String.valueOf(zcInfo.getInspectTime()));
        inspect.setLastCheckTime(new Date());
        inspect.setBz(null);
        inspect.setCreateBy(id);
        inspect.setUpdateBy(id);
        inspect.setCreateTime(new Date());
        inspect.setUpdateTime(new Date());
        inspect.setCheckTime(new Date());
        inspect.setCheckUserId(id);
        inspect.setCheckUsername(UserUtil.getLoginUser().getUsername());
        inspect.setStatus("1");
        //正常状态
        inspect.setDel(0);
        inspect.setCheckDeptId(UserUtil.getLoginUser().getDeptid());
        inspect.setCheckDeptName(UserUtil.getLoginUser().getLoginUserDepartName());
        inspect.setCode("");
        int result =  zcInspectDao.save(inspect);
        if(result>0){
            insertInspectRelation(zcInfo,inspect);
        }else{
            log.error("插入数据失败,资产id=={}",zcInfo.getId());
        }
        return result;
    }

    private void  insertInspectRelation(ZcInfo zcInfo,ZcInspect zcInspect){
        Map<String,Object> map  = new HashMap<>();
        map.put("zc_inspect_id",zcInspect.getId());
        map.put("zc_id",zcInfo.getId());
        map.put("status",0);
        zcInspectDao.saveZcInspectRel(map);
    }




    @Override
    public void delete(Long id) {
        ZcInfo zcInfo = new ZcInfo();
        zcInfo.setId(id);
        zcInfo.setUpdateBy(UserUtil.getLoginUser().getId());
        zcInfo.setDel(1);
        zcInfoDao.update(zcInfo);
        log.debug("删除资产档案{}", zcInfo.getUpdateBy() + zcInfo.getZcName());
    }

    @Override
    public void export(PageTableRequest request, HttpServletResponse response) {
        request.getParams().put("del", "0");
        // 使用部门
        Object syDeptId = request.getParams().get("syDeptId");
        Object glDeptId = request.getParams().get("glDeptId");
        request.getParams().put("syDeptId", UserUtil.getLoginUser().getDeptid());
        request.getParams().put("glDeptId", UserUtil.getLoginUser().getDeptid());
        if(permissionDao.hasPermission(UserUtil.getLoginUser().getId(),"sys:zcInfo:querysydept") > 0){
            request.getParams().put("syRole", "syRole");
        }
        if(permissionDao.hasPermission(UserUtil.getLoginUser().getId(),"sys:zcInfo:querygldept") > 0){
            request.getParams().put("glRole", "glRole");
            if (!ObjectUtils.isEmpty(glDeptId)) {
                request.getParams().put("glDeptId", glDeptId);
            }else {
                request.getParams().put("glDeptId", UserUtil.getLoginUser().getDeptid());
            }
            if (!ObjectUtils.isEmpty(syDeptId)) {
                request.getParams().put("syDeptId", syDeptId);
            }else {
                request.getParams().put("syDeptId", UserUtil.getLoginUser().getDeptid());
            }
        }
        // 财务部门
        if(permissionDao.hasPermission(UserUtil.getLoginUser().getId(),"sys:zcInfo:queryall") > 0){
            request.getParams().put("syRole", null);
            request.getParams().put("glRole", null);
            if (!ObjectUtils.isEmpty(syDeptId)) {
                request.getParams().put("searchSyDeptId", "searchSyDeptId");
                request.getParams().put("syDeptId", syDeptId);
            }else {
                request.getParams().put("syDeptId", null);
            }
            if (!ObjectUtils.isEmpty(glDeptId)) {
                request.getParams().put("searchGlDeptId", "searchGlDeptId");
                request.getParams().put("glDeptId", glDeptId);
            }else {
                request.getParams().put("glDeptId", null);
            }
        }
        if (!ObjectUtils.isEmpty(syDeptId)) {
            request.getParams().put("glRole", null);
            request.getParams().put("syDeptId", syDeptId);
            request.getParams().put("searchSyDeptId", "searchSyDeptId");
        }
        if (!ObjectUtils.isEmpty(glDeptId)) {
            request.getParams().put("glRole", null);
            request.getParams().put("glDeptId", glDeptId);
            request.getParams().put("searchGlDeptId", "searchGlDeptId");
        }
        List<ZcInfoDto> zcInfoList = zcInfoDao.listByCondition(request.getParams());
        String fileName = "资产档案";
        if ("90".equals(request.getParams().get("daoqi"))) fileName = "到期资产";
        if ("90".equals(request.getParams().get("linbao"))) fileName = "临保资产";
//        if (!CollectionUtils.isEmpty(equipmentList)) {
        String[] headers = new String[]{"资产追溯码", "卡片编号", "资产编码", "资产名称", "资产分类", "规格", "型号", "单位", "使用状态", "管理部门", "使用部门", "使用人", "存放地点", "维护周期/天"
                , "资产来源", "原价值", "累计折旧", "本年折旧", "净值", "减值准备", "净额", "净残值率", "净残值", "开始使用日期", "使用月限", "已计提期数"
                , "剩余期限", "服务商名称", "联系人", "联系方式", "服务商地址", "保修期限", "创建人", "创建时间", "备注"};

        List<Object[]> datas = new ArrayList<>(zcInfoList.size());
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat s1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (ZcInfoDto zcInfo : zcInfoList) {
            String StartUseTime = "";
            String createTime = "";
            if (zcInfo.getStartUseTime() != null) {
                StartUseTime = s.format(zcInfo.getStartUseTime());
            }
            if (zcInfo.getCreateTime() != null) {
                createTime = s1.format(zcInfo.getCreateTime());
            }
            Object[] objects = new Object[]{zcInfo.getEpcid(), zcInfo.getCardNum(), zcInfo.getZcCodenum(), zcInfo.getZcName(), zcInfo.getZcCategoryName(), zcInfo.getSpecification(), zcInfo.getModel()
                    , zcInfo.getUnit(), zcInfo.getUseStatusName(), zcInfo.getGlDeptName(), zcInfo.getSyDeptName(), zcInfo.getSyName(), zcInfo.getStoreAddress()
                    , zcInfo.getMaintainCycle(), zcInfo.getZcFrom(), zcInfo.getOriginalValue(), zcInfo.getLjZhejiu(), zcInfo.getBnZhejiu(), zcInfo.getNetvalue()
                    , zcInfo.getJzzb(), zcInfo.getNet(), zcInfo.getNetResidualRate(), zcInfo.getNetResidualValue(), StartUseTime, zcInfo.getUseMonths()
                    , zcInfo.getHaveCount(), zcInfo.getRemainingperiod(), zcInfo.getCname(), zcInfo.getVenperson()
                    , zcInfo.getVenphone(), zcInfo.getVenaddress(), zcInfo.getWarrantyperiod(), zcInfo.getCreator(), createTime, zcInfo.getBz()
            };
            datas.add(objects);
        }

        ExcelUtil.excelExport(
                fileName, headers,
                datas, response);
//        }
        log.debug("资产档案导出{}", UserUtil.getLoginUser().getId());
    }

    @Override
    public Map Import(MultipartFile file) throws IOException {
        Map map = new HashMap();
        ImportParams params = new ImportParams();
        List<ZcInfoDto> list = new ArrayList<>();
        List<ZcInfo> insertlist = new ArrayList<>();
        FileInfo fileInfo = null;
        try {
            params.setTitleRows(5);
            params.setHeadRows(0);
//            params.setNeedVerfiy(true);
            list = ExcelImportUtil.importExcel(file.getInputStream(), ZcInfoDto.class, params);
            if (!CollectionUtils.isEmpty(list)) {
                List<ZcCategory> zcCategoryList = zcCategoryDao.listAll();
                List<Dept> deptList = deptDao.listDepts();
//                List<Dict> useStatusList = dictDao.listByType("useStatus");
                LoginUser loginUser = UserUtil.getLoginUser();
                for (int i = 0; i < list.size(); i++) {
                    ZcInfoDto zcInfo = list.get(i);
                    if ("合计".equals(zcInfo.getCardNum().trim())) continue;  //卡片编号值如果是合计就跳过
                    //判断必需的数据是否为空
                    if (zcInfo.getZcName() == null || "".equals(zcInfo.getZcName())) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行资产名称不能为空");
                        return map;
                    }
                    if (zcInfo.getZcCategoryName() == null || "".equals(zcInfo.getZcCategoryName())) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行资产分类不能为空");
                        return map;
                    }
                    List<ZcCategory> zcCategories = findZcCategory(zcCategoryList, zcInfo.getZcCategoryName());  //根据资产分类名称查找资产分类id
                    if (CollectionUtils.isEmpty(zcCategories)) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行资产分类不存在");
                        return map;
                    }
                    ZcCategory zcCategory = zcCategories.get(0);
                    zcInfo.setZcCategoryId(zcCategory.getId());

                    if (zcInfo.getGlDeptName() == null || "".equals(zcInfo.getGlDeptName())) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行管理部门不能为空");
                        return map;
                    }
                    List<Dept> glDepts = findDept(deptList, zcInfo.getGlDeptName());  //根据管理部门名称查找管理部门id
                    if (CollectionUtils.isEmpty(glDepts)) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行管理部门不存在");
                        return map;
                    }
                    Dept glDept = glDepts.get(0);
                    zcInfo.setGlDeptId(glDept.getId());

                    if (zcInfo.getSyDeptName() == null || "".equals(zcInfo.getSyDeptName())) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行使用部门不能为空");
                        return map;
                    }
                    List<Dept> syDepts = findDept(deptList, zcInfo.getSyDeptName());  //根据使用部门名称查找使用部门id
                    if (CollectionUtils.isEmpty(syDepts)) {
                        map.put("code", "1");
                        map.put("msg", "第" + (i + 6) + "行使用部门不存在");
                        return map;
                    }
                    Dept syDept = syDepts.get(0);
                    zcInfo.setSyDeptId(syDept.getId());

//                    if (zcInfo.getUseStatusName() == null || "".equals(zcInfo.getUseStatusName())){
//                        map.put("code","1");
//                        map.put("msg","第"+(i+2)+"行使用状态不能为空");
//                        return map;
//                    }
//                    List<Dict> useStatuss = findDict(useStatusList,zcInfo.getUseStatusName());  //根据使用部门名称查找使用部门id
//                    if (CollectionUtils.isEmpty(useStatuss)) {
//                        map.put("code","1");
//                        map.put("msg","第"+(i+2)+"行使用状态不存在");
//                        return map;
//                    }
//                    Dict useStatus = useStatuss.get(0);
//                    zcInfo.setUseStatus(Integer.valueOf(useStatus.getK()));

                    zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                    zcInfo.setCreateBy(loginUser.getId());
                    zcInfo.setDel(0);
                    zcInfo.setBf("0");
                    zcInfo.setAccountentryStatus(1);
                    zcInfo.setCardStatus(1);
                    ZcInfo insertZcInfo = (ZcInfo) zcInfo;
                    insertlist.add(insertZcInfo);

                    Integer quantity = zcInfo.getQuantity();   //资产数量
                    if (quantity > 1) {     //数量大于1，复制导入资产
                        for (int a = 0; a < quantity - 1; a++) {
                            ZcInfo zcInfo1 = new ZcInfo();
                            BeanUtils.copyProperties(insertZcInfo, zcInfo1);
                            zcInfo1.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                            insertlist.add(zcInfo1);
                        }
                    }
                }

                zcInfoDao.saves(insertlist);
                map.put("code", "0");
//                fileInfo = fileService.save(file);
            }
        } catch (Exception e) {
            map.put("code", "-1");
            map.put("msg", e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("资产档案导入{}", UserUtil.getLoginUser().getId());
            return map;
        }
    }

    @Override
    public Map gudingImport(MultipartFile file) throws IOException {
        Map map = new HashMap();
        ImportParams params = new ImportParams();
        List<GudingZcInfo> list = new ArrayList<>();
        List<ZcInfo> insertlist = new ArrayList<>();
        FileInfo fileInfo = null;
        // 统计该县的资产数
        LoginUser loginUser = UserUtil.getLoginUser();
        long deptid = loginUser.getDeptid();
        Dept dept = deptDao.getById(deptid);
        Dept pDept = deptDao.getById(dept.getPid());
        int count = zcInfoDao.countByDeptcode(pDept.getDeptcode());
        try {
            params.setHeadRows(0);
            params.setTitleRows(2);
            //params.setStartSheetIndex(1);
            //params.setNeedVerfiy(true);
            list = ExcelImportUtil.importExcel(file.getInputStream(), GudingZcInfo.class, params);
            if (!CollectionUtils.isEmpty(list)) {
                // 资产编码集合
                List<String> codes = list.stream().map(e -> e.getZcCodenum()).collect(Collectors.toList());
                List<String> existsCodes = zcInfoDao.listByZcCodeNum(codes);
                ArrayList<GudingZcInfo> objects = new ArrayList<>();
                for (GudingZcInfo gudingZcInfo : list) {
                    String zcCodenum = gudingZcInfo.getZcCodenum();
                    if (!existsCodes.contains(zcCodenum)) {
                        objects.add(gudingZcInfo);
                    }
                }
                list = objects;
                if (list.size()>1) {
                    List<ZcCategory> zcCategoryList = zcCategoryDao.listAll();
                    List<Dept> deptList = deptDao.listDepts();
                    //List<Dict> useStatusList = dictDao.listByType("useStatus");
                    for (int i = 0; i < list.size(); i++) {
                        GudingZcInfo zcInfo = list.get(i);
                        if ("合计".equals(zcInfo.getCardNum().trim())) continue;  //卡片编号值如果是合计就跳过
                        //判断必需的数据是否为空
//                    if (zcInfo.getEpcid() == null || "".equals(zcInfo.getEpcid())){
//                        map.put("code","1");
//                        map.put("msg","第"+(i+6)+"行资产追溯码不能为空");
//                        return map;
//                    }
                        if (zcInfo.getZcName() == null || "".equals(zcInfo.getZcName())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行资产名称不能为空");
                            return map;
                        }
                        if (zcInfo.getZcCategoryCode() == null || "".equals(zcInfo.getZcCategoryCode())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行资产分类不能为空");
                            return map;
                        }
                        List<ZcCategory> zcCategories = findZcCategory(zcCategoryList, zcInfo.getZcCategoryCode());  //根据资产分类名称查找资产分类id
                        if (CollectionUtils.isEmpty(zcCategories)) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行资产分类不存在");
                            return map;
                        }
                        ZcCategory zcCategory = zcCategories.get(0);
                        zcInfo.setZcCategoryId(zcCategory.getId());

                        if (zcInfo.getGlDeptName() == null || "".equals(zcInfo.getGlDeptName())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行管理部门不能为空");
                            return map;
                        }
                        List<Dept> glDepts = findDept(deptList, zcInfo.getGlDeptName());  //根据管理部门名称查找管理部门id
                        if (CollectionUtils.isEmpty(glDepts)) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行管理部门不存在");
                            return map;
                        }
                        Dept glDept = glDepts.get(0);
                        zcInfo.setGlDeptId(glDept.getId());

                        if (zcInfo.getSyDeptName() == null || "".equals(zcInfo.getSyDeptName())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行使用部门不能为空");
                            return map;
                        }
                        List<Dept> syDepts = findDept(deptList, zcInfo.getSyDeptName());  //根据使用部门名称查找使用部门id
                        if (CollectionUtils.isEmpty(syDepts)) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行使用部门不存在");
                            return map;
                        }
                        Dept syDept = syDepts.get(0);
                        Integer quantity1 = zcInfo.getQuantity();
                        if (zcInfo.getQuantity() > 1) {
                            BigDecimal originalValue = zcInfo.getOriginalValue();
                            BigDecimal netvalue = zcInfo.getNetvalue();
                            BigDecimal net = zcInfo.getNet();
                            BigDecimal subOriginalValue = originalValue.divide(new BigDecimal(quantity1), 2, RoundingMode.HALF_UP);
                            BigDecimal subNetvalue = netvalue.divide(new BigDecimal(quantity1), 2, RoundingMode.HALF_UP);
                            BigDecimal subNet = net.divide(new BigDecimal(quantity1), 2, RoundingMode.HALF_UP);
                            zcInfo.setOriginalValue(subOriginalValue);
                            zcInfo.setNetvalue(subNetvalue);
                            zcInfo.setNet(subNet);
                            for (int j = 0; j < quantity1 - 1; j++) {
                                ZcInfo info = new ZcInfo();
                                zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                                zcInfo.setSyDeptId(syDept.getId());
                                zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                                zcInfo.setCreateBy(loginUser.getId());
                                zcInfo.setDel(0);
                                zcInfo.setBf("0");
                                zcInfo.setAccountentryStatus(1);
                                zcInfo.setCardStatus(1);
                                zcInfo.setUseStatus(1);
                                BeanUtils.copyProperties(zcInfo, info);
                                insertlist.add(info);
                            }
                        }
                        zcInfo.setSyDeptId(syDept.getId());
                        zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                        zcInfo.setCreateBy(loginUser.getId());
                        zcInfo.setDel(0);
                        zcInfo.setBf("0");
                        zcInfo.setAccountentryStatus(1);
                        zcInfo.setCardStatus(1);
                        zcInfo.setUseStatus(1);
                        ZcInfo zc = new ZcInfo();
                        BeanUtils.copyProperties(zcInfo, zc);
                        insertlist.add(zc);
                    }

                    // 资产追溯码
                    ArrayList<ZcEpcCode> zcEpcCodeList = new ArrayList<>();
                    // 插入资产追溯码
                    for (int i = 0; i < insertlist.size(); i++) {
                        String epcId = EcpIdUtil.getStaticNum(count + (i + 1), 5);
                        insertlist.get(i).setEpcid(dept.getSuCode() + epcId);
                        insertlist.get(i).setCatType(0);
                        ZcEpcCode zcEpcCode = new ZcEpcCode();
                        zcEpcCode.setEpcid(dept.getSuCode()+epcId);
                        zcEpcCode.setDeptId(insertlist.get(i).getSyDeptId());
                        zcEpcCode.setEnable(0);
                        zcEpcCode.setCreateTime(new Date());
                        zcEpcCode.setUpdateTime(new Date());
                        zcEpcCodeList.add(zcEpcCode);
                    }
                    zcEpcCodeDao.saves(zcEpcCodeList);
                    zcInfoDao.saves(insertlist);
                    map.put("code", "0");
//                fileInfo = fileService.save(file);
                }else {
                    map.put("code", "0");
                }
            }
        } catch (Exception e) {
            map.put("code", "-1");
            map.put("msg", e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("固定资产档案导入{}", UserUtil.getLoginUser().getId());
            return map;
        }
    }

    @Override
    public Map dizhiImport(MultipartFile file) throws IOException {
        Map map = new HashMap();
        ImportParams params = new ImportParams();
        List<DizhiZcInfo> list = new ArrayList<>();
        List<ZcInfo> insertlist = new ArrayList<>();
        FileInfo fileInfo = null;
        // 统计该县的资产数
        LoginUser loginUser = UserUtil.getLoginUser();
        long deptid = loginUser.getDeptid();
        Dept dept = deptDao.getById(deptid);
        Dept pDept = deptDao.getById(dept.getPid());
        int count = zcInfoDao.countByDeptcode(pDept.getDeptcode());
        try {
            params.setHeadRows(0);
            params.setTitleRows(2);
//            params.setNeedVerfiy(true);
            list = ExcelImportUtil.importExcel(file.getInputStream(), DizhiZcInfo.class, params);
            if (!CollectionUtils.isEmpty(list)) {
                List<ZcCategory> zcCategoryList = zcCategoryDao.listAll();
                List<Dept> deptList = deptDao.listDepts();
                // 资产编码集合
                List<String> codes = list.stream().map(e -> e.getZcCodenum()).collect(Collectors.toList());
                List<String> existsCodes = zcInfoDao.listByZcCodeNum(codes);
                ArrayList<DizhiZcInfo> objects = new ArrayList<>();
                for (DizhiZcInfo dizhiZcInfo : list) {
                    String zcCodenum = dizhiZcInfo.getZcCodenum();
                    if (!existsCodes.contains(zcCodenum)) {
                        objects.add(dizhiZcInfo);
                    }
                }
                list = objects;
                if (list.size()>1) {
                    //                List<Dict> useStatusList = dictDao.listByType("useStatus");
                    for (int i = 0; i < list.size(); i++) {
                        DizhiZcInfo zcInfo = list.get(i);
                        if ("合计".equals(zcInfo.getCardNum().trim())) continue;  //资产编号值如果是合计就跳过
                        //判断必需的数据是否为空
//                    if (zcInfo.getEpcid() == null || "".equals(zcInfo.getEpcid())){
//                        map.put("code","1");
//                        map.put("msg","第"+(i+6)+"行资产追溯码不能为空");
//                        return map;
//                    }
                        if (zcInfo.getZcName() == null || "".equals(zcInfo.getZcName())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行资产名称不能为空");
                            return map;
                        }
                        if (zcInfo.getZcCategoryCode() == null || "".equals(zcInfo.getZcCategoryCode())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行资产分类不能为空");
                            return map;
                        }
                        List<ZcCategory> zcCategories = findZcCategory(zcCategoryList, zcInfo.getZcCategoryCode());  //根据资产分类名称查找资产分类id
                        if (CollectionUtils.isEmpty(zcCategories)) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行资产分类不存在");
                            return map;
                        }
                        ZcCategory zcCategory = zcCategories.get(0);
                        zcInfo.setZcCategoryId(zcCategory.getId());

                        if (zcInfo.getGlDeptName() == null || "".equals(zcInfo.getGlDeptName())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行管理部门不能为空");
                            return map;
                        }
                        List<Dept> glDepts = findDeptgl(deptList, zcInfo.getGlDeptName());  //根据管理部门名称查找管理部门id
                        if (CollectionUtils.isEmpty(glDepts)) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行管理部门不存在");
                            return map;
                        }
                        Dept glDept = glDepts.get(0);
                        zcInfo.setGlDeptId(glDept.getId());

                        if (zcInfo.getSyDeptName() == null || "".equals(zcInfo.getSyDeptName())) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行使用部门不能为空");
                            return map;
                        }
                        List<Dept> syDepts = findDept(deptList, zcInfo.getSyDeptName());  //根据使用部门名称查找使用部门id
                        if (CollectionUtils.isEmpty(syDepts)) {
                            map.put("code", "1");
                            map.put("msg", "第" + (i + 6) + "行使用部门不存在");
                            return map;
                        }
                        // 处理数量
                        Dept syDept = syDepts.get(0);
                        Integer quantity1 = zcInfo.getQuantity();
                        if (zcInfo.getQuantity() > 1) {
                            BigDecimal originalValue = zcInfo.getOriginalValue();
                            BigDecimal netvalue = zcInfo.getNetvalue();
                            BigDecimal net = zcInfo.getNet();
                            BigDecimal subOriginalValue = originalValue.divide(new BigDecimal(quantity1), 2, RoundingMode.HALF_UP);
                            BigDecimal subNetvalue = netvalue.divide(new BigDecimal(quantity1), 2, RoundingMode.HALF_UP);
                            BigDecimal subNet = net.divide(new BigDecimal(quantity1), 2, RoundingMode.HALF_UP);
                            zcInfo.setOriginalValue(subOriginalValue);
                            zcInfo.setNetvalue(subNetvalue);
                            zcInfo.setNet(subNet);
                            for (int j = 0; j < quantity1 - 1; j++) {
                                ZcInfo info = new ZcInfo();
                                zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                                zcInfo.setSyDeptId(syDept.getId());
                                zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                                zcInfo.setCreateBy(loginUser.getId());
                                zcInfo.setDel(0);
                                zcInfo.setBf("0");
                                zcInfo.setAccountentryStatus(1);
                                zcInfo.setCardStatus(1);
                                zcInfo.setUseStatus(1);
                                BeanUtils.copyProperties(zcInfo, info);
                                insertlist.add(info);
                            }
                        }
                        zcInfo.setSyDeptId(syDept.getId());
                        zcInfo.setSelfCodenum(ZiChanCodeUtil.getZiChanCode());
                        zcInfo.setCreateBy(loginUser.getId());
                        zcInfo.setDel(0);
                        zcInfo.setBf("0");
                        zcInfo.setAccountentryStatus(1);
                        zcInfo.setCardStatus(1);
                        zcInfo.setUseStatus(1);
                        ZcInfo zc = new ZcInfo();
                        BeanUtils.copyProperties(zcInfo, zc);
                        insertlist.add(zc);
                    }

                    // 资产追溯码
                    ArrayList<ZcEpcCode> zcEpcCodeList = new ArrayList<>();
                    // 插入资产追溯码
                    for (int i = 0; i < insertlist.size(); i++) {
                        String epcId = EcpIdUtil.getStaticNum(count + (i + 1), 5);
                        insertlist.get(i).setEpcid(dept.getSuCode() + epcId);
                        insertlist.get(i).setCatType(1);
                        // 资产追溯码
                        ZcEpcCode zcEpcCode = new ZcEpcCode();
                        zcEpcCode.setEpcid(dept.getSuCode()+epcId);
                        zcEpcCode.setDeptId(insertlist.get(i).getSyDeptId());
                        zcEpcCode.setEnable(0);
                        zcEpcCode.setCreateTime(new Date());
                        zcEpcCode.setUpdateTime(new Date());
                        zcEpcCodeList.add(zcEpcCode);
                    }
                    zcEpcCodeDao.saves(zcEpcCodeList);
                    zcInfoDao.saves(insertlist);
                    map.put("code", "0");
                    //fileInfo = fileService.save(file);
                }else {
                    map.put("code", "0");
                }
            }
        } catch (Exception e) {
            map.put("code", "-1");
            map.put("msg", e.getMessage());
            e.printStackTrace();
        } finally {
            log.debug("低值易耗品资产档案导入{}", UserUtil.getLoginUser().getId());
            return map;
        }
    }


    List<ZcCategory> findZcCategory(List<ZcCategory> zcCategoryList, String zcCategoryCode) {
        List<ZcCategory> collect = new ArrayList<>();
        if (!CollectionUtils.isEmpty(zcCategoryList))
            collect = zcCategoryList.stream().filter(zc -> zcCategoryCode.equals(zc.getCode())).collect(Collectors.toList());
        return collect;
    }

    List<Dept> findDept(List<Dept> deptList, String deptname) {
        List<Dept> collect = new ArrayList<>();
        // String newDeptname = deptname.replace("垣曲农商行","");    //暂用
        if (!CollectionUtils.isEmpty(deptList))
            collect = deptList.stream().filter(d -> deptname.equals(d.getAllName())).collect(Collectors.toList());
        return collect;
    }

    List<Dept> findDeptgl(List<Dept> deptList, String deptname) {
        List<Dept> collect = new ArrayList<>();
        // String newDeptname = deptname.replace("垣曲农商银行","");    //暂用
        if (!CollectionUtils.isEmpty(deptList))
            collect = deptList.stream().filter(d -> deptname.equals(d.getOtherAllName())).collect(Collectors.toList());
        return collect;
    }

    List<Dict> findDict(List<Dict> dictList, String val) {
        List<Dict> collect = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dictList))
            collect = dictList.stream().filter(d -> val.equals(d.getVal())).collect(Collectors.toList());
        return collect;
    }


    @Autowired
    ZcRepairDao zcRepairDao;

    @Autowired
    ZcBfDao zcBfDao;

    @Autowired
    private ZcBuyDao zcBuyDao;

    @Autowired
    ZcDeployDao zcDeployDao;

    @Autowired
    private ZcCheckDao zcCheckDao;




    @Override
    public AppIndexZcValueAndNumber getZcValueAndZcNumber(SysUser user) {
        AppIndexZcValueAndNumber result = new AppIndexZcValueAndNumber();
        String auth = user.getC03();
        long id = user.getDeptid();
        if (("cwb").equals(auth)) {
            //财务部身份
            List<GlDeptZcCountVO> map = zcInfoDao.getGlDeptZcCount(id);
            log.info("获取的四个部门数据是===={}", map);
            if (!CollectionUtils.isEmpty(map)) {
                setDiffZcCount(result, map);
            }
            List<ZcInfoDto> list = zcInfoDao.queryAllDeptZcList(id);
            if (!CollectionUtils.isEmpty(list)) {
                int count = CollectionUtils.isEmpty(list) ? 0:list.size();
                result.setZcValue(getTotalValue(list));
                result.setZcCount(count);
            }
            setBenYueDeptCount(result, id);
        } else if (("kjb").equals(auth) || ("yyb").equals(auth)
                || ("zhb").equals(auth) || ("bwb").equals(auth)) {
            //综合部 运营部 综合办 保卫部
            List<ZcInfoDto> zcInfoDtoList = zcInfoDao.queryGlDeptZcList(id);
            if (!CollectionUtils.isEmpty(zcInfoDtoList)) {
                int count = CollectionUtils.isEmpty(zcInfoDtoList) ? zcInfoDtoList.size() : 0;
                result.setZcValue(getTotalValue(zcInfoDtoList));
                result.setZcCount(count);
            }
            List<GlDeptZcCountVO> map = zcInfoDao.getGlDeptZcCount(id);
            log.info("获取的四个部门数据是===={}", map);
            if (!CollectionUtils.isEmpty(map)) {
                setDiffZcCount(result, map);
            }
            setBenYueDeptCount(result, id);

        } else {
            //使用部门
            List<ZcInfoDto> list = zcInfoDao.getZcValueAndZcNumber(user.getId());
            int count = CollectionUtils.isEmpty(list) ? list.size() : 0;
            result.setZcCount(count);
            result.setZcValue(getTotalValue(list));
            List<GlDeptZcCountVO> mapList = zcInfoDao.getDifferentDeptZcCount(id);
            log.info("获取的四个部门数据是===={}", mapList);
            if (!CollectionUtils.isEmpty(mapList)) {
                setDiffZcCount(result, mapList);
            }
            int caigouCount = zcBuyDao.queryBuyCountById(id);
            int diaopeiCount = zcDeployDao.queryDeployCountById(id);
            int deptPandianCount = zcInspectDao.queryZcInspectCountByDeptId(id);
            int baoxiuCount = zcRepairDao.queryZcRepairById(id);
            int xunjianCount = zcCheckDao.queryZcCheckCountById(id);
            int chuzhiCount = zcBfDao.queryBaoFeiCountById(id);
            result.setDiaoboCount(diaopeiCount);
            result.setCaigouCount(caigouCount);
            result.setPandianCount(deptPandianCount);
            result.setChuzhiCount(chuzhiCount);
            result.setBaoxiuCount(baoxiuCount);
            result.setXunjianCount(xunjianCount);
        }
        return result;
    }

    @Override
    public ZcInfo queryZnInfoByEpcId(String epcid) {
        return zcInfoDao.queryZnInfoByEpcId(epcid);
    }

    @Override
    public List<ZcInfoDto> getAllZcInfoListByUser(SysUser sysUser, ZcInfoListDTO zcInfoListDTO) {
        Map<String, Object> map = new HashMap<>();
        map.put("page", zcInfoListDTO.getPage() * zcInfoListDTO.getLimit() - zcInfoListDTO.getLimit());
        map.put("limit", zcInfoListDTO.getLimit());
        map.put("keyword", zcInfoListDTO.getKeyword());
        map.put("deptid", sysUser.getDeptid());
        map.put("id", sysUser.getId());
        String auth = sysUser.getC03();
        List<ZcInfoDto> zcInfoList = null;
        if ("cwb".equals(auth)) {
            //获取使用部门的资产列表数据
            zcInfoList = zcInfoDao.getAllZcInfoListByCwb(map);
        } else if ("bwb".equals(auth) ||
                "kjb".equals(auth) ||
                "zhb".equals(auth) ||
                "yyb".equals(auth)) {
            zcInfoList = zcInfoDao.getAllZcInfoListByManager(map);
        } else {
            //获取普通部门的资产列表
            zcInfoList = zcInfoDao.getAllZcInfoListOrdinaryDept(map);
        }
        return zcInfoList;
    }

    @Override
    public ZcInfoDto queryZnInfoDtoByEpcId(String epcid) {
        return zcInfoDao.queryZnInfoDtoByEpcId(epcid);
    }

    @Override
    public ZongHangMonthNumber getZongHangZcNumber(SysUser sysUser) {

        ZongHangMonthNumber zongHangMonthNumber = new ZongHangMonthNumber();
        ArrayList<ZhiHangNumber> list = new ArrayList<>();
        // 查询全部在线资产
        Map<String, Object> params = new HashMap<>();
        params.put("useStatus",7);

        // 部门信息
        List<Dept> allDepts = deptDao.listByPid(11l);
        //List<Long> deptIds = allDepts.stream().map(e -> e.getId()).collect(Collectors.toList());
        //List<Long> glDeptIds = allDepts.stream().filter(e -> "3".equals(e.getZhfhgl())).map(e -> e.getId()).collect(Collectors.toList());
        //Collections.sort(glDeptIds);
        long zhb = 54l;
        long dzb = 57l;
        long yyb = 61l;
        long aqb = 62l;

        List<ZongHangZcInfo> zcInfoList = zcInfoDao.listZcInfoByCondition(params);
        BigDecimal totalValue = zcInfoList.stream()
                // 将user对象的age取出来map为Bigdecimal
                .map(e -> (e.getNetvalue()==null?new BigDecimal("0"):e.getNetvalue()))
                // 使用reduce()聚合函数,实现累加器
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        // 遍历部门
        for (Dept dept : allDepts) {
            ZhiHangNumber zhiHangNumber = new ZhiHangNumber();
            zhiHangNumber.setDetId(dept.getId());
            zhiHangNumber.setDetName(dept.getDeptname());
            int bwbZcCount = 0;
            int kjbZcCount = 0;
            int zhbZcCount = 0;
            int yybZcCount = 0;
            for (ZongHangZcInfo zongHangZcInfo : zcInfoList) {
                // totalValue.add(zongHangZcInfo.getNetvalue()==null?new BigDecimal("0"):zongHangZcInfo.getNetvalue());
                if (zongHangZcInfo.getSyDeptId().equals(dept.getId()) && zongHangZcInfo.getGlDeptId().equals(zhb)) {
                    zhbZcCount++;
                }
                if (zongHangZcInfo.getSyDeptId().equals(dept.getId()) && zongHangZcInfo.getGlDeptId().equals(dzb)) {
                    kjbZcCount++;
                }
                if (zongHangZcInfo.getSyDeptId().equals(dept.getId()) && zongHangZcInfo.getGlDeptId().equals(yyb)) {
                    yybZcCount++;
                }
                if (zongHangZcInfo.getSyDeptId().equals(dept.getId()) && zongHangZcInfo.getGlDeptId().equals(aqb)) {
                    bwbZcCount++;
                }
            }
            zhiHangNumber.setBwbZcCount(bwbZcCount);
            zhiHangNumber.setKjbZcCount(kjbZcCount);
            zhiHangNumber.setYybZcCount(yybZcCount);
            zhiHangNumber.setZhbZcCount(zhbZcCount);
            list.add(zhiHangNumber);
        }

        zongHangMonthNumber.setZcValue(totalValue);
        zongHangMonthNumber.setZcCount(zcInfoList.size());
        zongHangMonthNumber.setAssetsCountList(list);
        // 本月全部数据统计
        zongHangMonthNumber.setBaoxiuCount(zongHangBenYueDao.countMonthBaoxiuCount());
        zongHangMonthNumber.setCaigouCount(zongHangBenYueDao.countMonthCaigouCount());
        zongHangMonthNumber.setChuzhiCount(zongHangBenYueDao.countMonthChuzhiCount());
        zongHangMonthNumber.setDiaoboCount(zongHangBenYueDao.countMonthDiaoboCount());
        zongHangMonthNumber.setPandianCount(zongHangBenYueDao.countMonthPandianCount());
        zongHangMonthNumber.setXunjianCount(zongHangBenYueDao.countMonthXunjianCount());

        return zongHangMonthNumber;
    }


    private BigDecimal getTotalValue(List<ZcInfoDto> list) {
        BigDecimal total = new BigDecimal(BigDecimal.ZERO.intValue());
        if (CollectionUtils.isEmpty(list)) {
            return total;
        } else {
            for (ZcInfo zcInfo : list) {
                total = total.add(zcInfo.getNetvalue()==null?new BigDecimal("0"):zcInfo.getNetvalue());
            }
            return total;
        }
    }


    private void setDiffZcCount(AppIndexZcValueAndNumber result, List<GlDeptZcCountVO> mapList) {
        mapList.forEach(k -> {
            if ("kjb".equals(k.getC03())) {
                result.setKjbZcCount(k.getZcCount());
            } else if ("yyb".equals(k.getC03())) {
                result.setYybZcCount(k.getZcCount());
            } else if ("bwb".equals(k.getC03())) {
                result.setBwbZcCount(k.getZcCount());
            } else if ("zhb".equals(k.getC03())) {
                result.setZhbZcCount(k.getZcCount());
            }
        });
    }


    /**
     * 用于统计本月的数据信息
     */
    private void setBenYueDeptCount(AppIndexZcValueAndNumber result, long id) {
        int deptCaigouCount = zcBuyDao.queryDeptBuyCountById(id);
        int deptDiaopeiCount = zcDeployDao.queryDeptDeployCountById(id);
        int deptPandianCount = zcInspectDao.queryZcInspectCountByDeptId(id);
        int deptBaoxiuCount = zcRepairDao.queryDeptZcRepairById(id);
        int deptXunjianCount = zcCheckDao.queryDeptZcCheckCountById(id);
        int deptChuzhiCount = zcBfDao.queryDeptBaoFeiCountById(id);
        result.setDiaoboCount(deptDiaopeiCount);
        result.setCaigouCount(deptCaigouCount);
        result.setPandianCount(deptPandianCount);
        result.setChuzhiCount(deptChuzhiCount);
        result.setBaoxiuCount(deptBaoxiuCount);
        result.setXunjianCount(deptXunjianCount);
    }
}
