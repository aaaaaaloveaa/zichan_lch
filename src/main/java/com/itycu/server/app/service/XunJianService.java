package com.itycu.server.app.service;

import com.itycu.server.app.dto.xunjian.XunJianSubmitDTO;
import com.itycu.server.app.vo.xunjian.XunJianVO;
import com.itycu.server.model.SysUser;
import com.itycu.server.model.ZcInspectRecord;

import java.util.List;

public interface XunJianService {


    /**
     * 获取巡检的列表数据
     * @return
     */
    List<XunJianVO> getXunjianList(SysUser sysUser);


    /**
     * 插入巡检记录
     * @param xunJianSubmitDTO
     * @return
     */
    int insertInspectRecord(XunJianSubmitDTO xunJianSubmitDTO);


    List<XunJianVO>  getInspectRecordList(SysUser sysUser);


    ZcInspectRecord  getInspectRecordById(int id);

}
