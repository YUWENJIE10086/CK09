package com.barn.barn.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.barn.barn.entity.MaintenanceTeam;
import com.barn.barn.mapper.MaintenanceTeamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceTeamService {

    @Autowired
    private MaintenanceTeamMapper maintenanceTeamMapper;

    public List<MaintenanceTeam> listAll(String teamType, String countyCode) {
        LambdaQueryWrapper<MaintenanceTeam> wrapper = new LambdaQueryWrapper<>();
        if (teamType != null && !teamType.isEmpty()) {
            wrapper.eq(MaintenanceTeam::getTeamType, teamType);
        }
        if (countyCode != null && !countyCode.isEmpty()) {
            wrapper.eq(MaintenanceTeam::getCountyCode, countyCode);
        }
        wrapper.orderByDesc(MaintenanceTeam::getRepairCount);
        return maintenanceTeamMapper.selectList(wrapper);
    }

    public MaintenanceTeam getById(Long id) {
        return maintenanceTeamMapper.selectById(id);
    }

    public int save(MaintenanceTeam team) {
        return maintenanceTeamMapper.insert(team);
    }

    public int update(MaintenanceTeam team) {
        return maintenanceTeamMapper.updateById(team);
    }

    public int delete(Long id) {
        return maintenanceTeamMapper.deleteById(id);
    }
}
