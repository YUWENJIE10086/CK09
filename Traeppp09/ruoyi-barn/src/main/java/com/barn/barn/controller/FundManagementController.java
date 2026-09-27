package com.barn.barn.controller;

import com.barn.barn.entity.FundManagement;
import com.barn.barn.service.FundManagementService;
import com.barn.common.core.domain.R;
import com.barn.common.core.domain.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/barn/fund")
public class FundManagementController {

    @Autowired
    private FundManagementService fundService;

    @GetMapping("/list")
    public TableDataInfo<FundManagement> list(@RequestParam(required = false) Integer fundYear,
                                              @RequestParam(required = false) String countyCode) {
        List<FundManagement> list = fundService.listAll(fundYear, countyCode);
        return TableDataInfo.build(list, list.size(), 1, list.size());
    }

    @GetMapping("/{id}")
    public R<FundManagement> getInfo(@PathVariable Long id) {
        return R.ok(fundService.getById(id));
    }

    @PostMapping
    public R<Void> add(@RequestBody FundManagement fund) {
        fundService.save(fund);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody FundManagement fund) {
        fundService.update(fund);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        fundService.delete(id);
        return R.ok();
    }
}
