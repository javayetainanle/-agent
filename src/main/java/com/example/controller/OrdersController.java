package com.example.controller;

import cn.hutool.core.lang.Dict;
import com.example.common.Result;
import com.example.entity.Orders;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.service.OrdersService;
import com.example.utils.GeoUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车前端操作接口
 **/
@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Resource
    private OrdersService ordersService;

    @Resource
    private UserMapper userMapper;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Orders orders) {
        ordersService.add(orders);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        ordersService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        ordersService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Orders orders) {
        ordersService.updateById(orders);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Orders orders = ordersService.selectById(id);
        return Result.success(orders);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Orders orders ) {
        List<Orders> list = ordersService.selectAll(orders);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Orders orders,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Orders> page = ordersService.selectPage(orders, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 分页查询（带距离）
     * 买家端传入userLat/userLng（用户当前位置），计算用户到商家的距离
     * 商家端不传userLat/userLng，从user表查买家位置算距离
     */
    @GetMapping("/selectPageWithDistance")
    public Result selectPageWithDistance(Orders orders,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Double userLat,
                                         @RequestParam(required = false) Double userLng) {
        PageInfo<Orders> page = ordersService.selectPage(orders, pageNum, pageSize);
        for (Orders o : page.getList()) {
            double dist = -1;
            if (userLat != null && userLng != null) {
                // 买家端：用传入的用户位置算距离
                dist = GeoUtils.calculateDistance(userLat, userLng, o.getBusinessLatitude(), o.getBusinessLongitude());
            } else if (o.getUserId() != null) {
                // 商家端：从user表查买家位置算距离
                User buyer = userMapper.selectById(o.getUserId());
                if (buyer != null) {
                    dist = GeoUtils.calculateDistance(buyer.getLatitude(), buyer.getLongitude(), o.getBusinessLatitude(), o.getBusinessLongitude());
                }
            }
            o.setDistance(dist);
            o.setDistanceText(GeoUtils.formatDistance(dist));
        }
        return Result.success(page);
    }

}