package com.barn.barn.controller;

import com.barn.barn.entity.MaintenanceTeam;
import com.barn.barn.service.MaintenanceTeamService;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/barn/team")
public class MaintenanceTeamController {

    @Autowired
    private MaintenanceTeamService teamService;

    @GetMapping("/list")
    public TableDataInfo<MaintenanceTeam> list(@RequestParam(required = false) String teamType,
                                               @RequestParam(required = false) String countyCode) {
        List<MaintenanceTeam> list = teamService.listAll(teamType, countyCode);
        return TableDataInfo.build(list, list.size(), 1, list.size());
    }

    @GetMapping("/{id}")
    public R<MaintenanceTeam> getInfo(@PathVariable Long id) {
        return R.ok(teamService.getById(id));
    }

    @PostMapping
    public R<Void> add(@RequestBody MaintenanceTeam team) {
        teamService.save(team);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody MaintenanceTeam team) {
        teamService.update(team);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        teamService.delete(id);
        return R.ok();
    }
}
