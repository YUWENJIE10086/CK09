package com.barn.barn.controller;

import com.barn.barn.entity.SysCounty;
import com.barn.barn.entity.SysProjectType;
import com.barn.barn.entity.SysTownship;
import com.barn.barn.entity.SysVillage;
import com.barn.barn.service.SysCountyService;
import com.barn.barn.service.SysProjectTypeService;
import com.barn.barn.service.SysTownshipService;
import com.barn.barn.service.SysVillageService;
import com.barn.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 区域/类型字典Controller
 */
@RestController
@RequestMapping("/barn/dict")
public class DictController {

    @Autowired
    private SysCountyService countyService;
    @Autowired
    private SysTownshipService townshipService;
    @Autowired
    private SysVillageService villageService;
    @Autowired
    private SysProjectTypeService projectTypeService;

    @GetMapping("/county")
    public R<List<SysCounty>> listCounty(@RequestParam(required = false) String cityCode) {
        return R.ok(countyService.listAll(cityCode));
    }

    @GetMapping("/township")
    public R<List<SysTownship>> listTownship(@RequestParam(required = false) String countyCode) {
        return R.ok(townshipService.listAll(countyCode));
    }

    @GetMapping("/village")
    public R<List<SysVillage>> listVillage(@RequestParam(required = false) String townCode) {
        return R.ok(villageService.listAll(townCode));
    }

    @GetMapping("/projectType")
    public R<List<SysProjectType>> listProjectType() {
        return R.ok(projectTypeService.listAll());
    }
}
