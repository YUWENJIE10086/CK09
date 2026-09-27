package com.barn.barn.service;

import com.barn.barn.entity.SysProjectType;
import com.barn.barn.mapper.SysProjectTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysProjectTypeService {

    @Autowired
    private SysProjectTypeMapper sysProjectTypeMapper;

    public List<SysProjectType> listAll() {
        return sysProjectTypeMapper.selectList(null);
    }
}
