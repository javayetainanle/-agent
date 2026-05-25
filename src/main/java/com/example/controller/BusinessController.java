package com.example.controller;

import com.example.common.Result;
import com.example.entity.Business;
import com.example.service.BusinessService;
import com.example.utils.GeoUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家前端操作接口
 **/
@RestController
@RequestMapping("/business")
public class BusinessController {

    @Resource
    private BusinessService businessService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Business business) {
        businessService.add(business);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        businessService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        businessService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Business business) {
        businessService.updateById(business);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Business business = businessService.selectById(id);
        return Result.success(business);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Business business ) {
        List<Business> list = businessService.selectAll(business);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Business business,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Business> page = businessService.selectPage(business, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 计算用户到指定商家的距离
     */
    @GetMapping("/distance")
    public Result calculateDistance(@RequestParam Integer businessId,
                                    @RequestParam Double userLat,
                                    @RequestParam Double userLng) {
        Business business = businessService.selectById(businessId);
        if (business == null) {
            return Result.error("400", "商家不存在");
        }
        double distance = GeoUtils.calculateDistance(userLat, userLng, business.getLatitude(), business.getLongitude());
        Map<String, Object> data = new HashMap<>();
        data.put("distance", distance);
        data.put("distanceText", GeoUtils.formatDistance(distance));
        return Result.success(data);
    }

    /**
     * 查询所有商家并附带与当前用户的距离
     */
    @GetMapping("/selectAllWithDistance")
    public Result selectAllWithDistance(Business business,
                                        @RequestParam(required = false) Double userLat,
                                        @RequestParam(required = false) Double userLng) {
        List<Business> list = businessService.selectAll(business);
        if (userLat != null && userLng != null) {
            for (Business b : list) {
                double dist = GeoUtils.calculateDistance(userLat, userLng, b.getLatitude(), b.getLongitude());
                b.setDistance(dist);
                b.setDistanceText(GeoUtils.formatDistance(dist));
            }
        }
        return Result.success(list);
    }

}