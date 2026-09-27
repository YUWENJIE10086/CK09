package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.SysVillage;
import com.barn.barn.mapper.SysVillageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysVillageService {

    @Autowired
    private SysVillageMapper sysVillageMapper;

    public List<SysVillage> listAll(String townCode) {
        LambdaQueryWrapper<SysVillage> wrapper = new LambdaQueryWrapper<>();
        if (townCode != null && !townCode.isEmpty()) {
            wrapper.eq(SysVillage::getTownshipCode, townCode);
        }
        wrapper.orderByAsc(SysVillage::getVillageCode);
        return sysVillageMapper.selectList(wrapper);
    }
}
