package com.example.controller;

import com.example.common.Result;
import com.example.entity.Goods;
import com.example.service.GoodsService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 商品信息表前端操作接口
 **/
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Goods goods) {
        goodsService.add(goods);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        goodsService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        goodsService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Goods goods) {
        goodsService.updateById(goods);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id) {
        Goods goods = goodsService.selectById(id);
        setWarning(goods);
        return Result.success(goods);
    }

    @GetMapping("/selectTop5")
    public Result selectTop5() {
        List<Goods> list = goodsService.selectTop5();
        list.removeIf(g -> g.getStatus() != null && g.getStatus() == 1);
        setWarning(list);
        return Result.success(list);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Goods goods ) {
        List<Goods> list = goodsService.selectAll(goods);
        setWarning(list);
        return Result.success(list);
    }

    @GetMapping("/selectByTypeId")
    public Result selectByTypeId(@RequestParam Integer id) {
        List<Goods> list = goodsService.selectByTypeId(id);
        list.removeIf(g -> g.getStatus() != null && g.getStatus() == 1);
        setWarning(list);
        return Result.success(list);
    }

    @GetMapping("/selectByName")
    public Result selectByName(@RequestParam String name) {
        List<Goods> list = goodsService.selectByName(name);
        list.removeIf(g -> g.getStatus() != null && g.getStatus() == 1);
        setWarning(list);
        return Result.success(list);
    }

    @GetMapping("/selectByBusinessId")
    public Result selectByBusinessId(@RequestParam Integer id) {
        List<Goods> list = goodsService.selectByBusinessId(id);
        list.removeIf(g -> g.getStatus() != null && g.getStatus() == 1);
        setWarning(list);
        return Result.success(list);
    }

    @GetMapping("/recommend")
    public Result recommend() {
        List<Goods> list = goodsService.recommend();
        list.removeIf(g -> g.getStatus() != null && g.getStatus() == 1);
        setWarning(list);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Goods notice,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Goods> page = goodsService.selectPage(notice, pageNum, pageSize);
        setWarning(page.getList());
        return Result.success(page);
    }

    /**
     * 商家临期预警检查：更新当前商家所有商品的is_warning字段，过期商品下架，返回预警信息
     */
    @GetMapping("/checkWarningByBusiness")
    public Result checkWarningByBusiness(@RequestParam Integer businessId) {
        Goods query = new Goods();
        query.setBusinessId(businessId);
        List<Goods> list = goodsService.selectAll(query);
        List<String> warnings = new ArrayList<>();
        int offShelfCount = 0;

        for (Goods goods : list) {
            int oldWarning = goods.getIsWarning() != null ? goods.getIsWarning() : 0;
            setWarning(goods);
            int newWarning = goods.getIsWarning() != null ? goods.getIsWarning() : 0;

            // 更新is_warning到数据库
            if (newWarning != oldWarning) {
                Goods update = new Goods();
                update.setId(goods.getId());
                update.setIsWarning(newWarning);
                goodsService.updateById(update);
            }

            if (newWarning == 1) {
                warnings.add("「" + goods.getName() + "」临期");
            } else if (newWarning == 2) {
                warnings.add("「" + goods.getName() + "」过期");
                // 过期商品下架：status设为1
                if (goods.getStatus() == null || goods.getStatus() == 0) {
                    Goods offShelf = new Goods();
                    offShelf.setId(goods.getId());
                    offShelf.setStatus(1);
                    goodsService.updateById(offShelf);
                    offShelfCount++;
                }
            }
        }

        StringBuilder msg = new StringBuilder();
        if (warnings.isEmpty()) {
            msg.append("所有商品状态正常");
        } else {
            msg.append(String.join("，", warnings));
            if (offShelfCount > 0) {
                msg.append("。已自动下架").append(offShelfCount).append("件过期商品");
            }
        }
        return Result.success(msg.toString());
    }

    /**
     * 管理员临期预警检查：更新所有商品的is_warning字段，过期商品下架，返回预警信息
     */
    @GetMapping("/checkWarningAll")
    public Result checkWarningAll() {
        List<Goods> list = goodsService.selectAll(new Goods());
        List<String> warnings = new ArrayList<>();
        int offShelfCount = 0;

        for (Goods goods : list) {
            int oldWarning = goods.getIsWarning() != null ? goods.getIsWarning() : 0;
            setWarning(goods);
            int newWarning = goods.getIsWarning() != null ? goods.getIsWarning() : 0;

            // 更新is_warning到数据库
            if (newWarning != oldWarning) {
                Goods update = new Goods();
                update.setId(goods.getId());
                update.setIsWarning(newWarning);
                goodsService.updateById(update);
            }

            if (newWarning == 1) {
                warnings.add(goods.getBusinessName() + "的「" + goods.getName() + "」临期");
            } else if (newWarning == 2) {
                warnings.add(goods.getBusinessName() + "的「" + goods.getName() + "」过期");
                // 过期商品下架：status设为1
                if (goods.getStatus() == null || goods.getStatus() == 0) {
                    Goods offShelf = new Goods();
                    offShelf.setId(goods.getId());
                    offShelf.setStatus(1);
                    goodsService.updateById(offShelf);
                    offShelfCount++;
                }
            }
        }

        StringBuilder msg = new StringBuilder();
        if (warnings.isEmpty()) {
            msg.append("所有商品状态正常");
        } else {
            msg.append(String.join("；", warnings));
            if (offShelfCount > 0) {
                msg.append("。已自动下架").append(offShelfCount).append("件过期商品");
            }
        }
        return Result.success(msg.toString());
    }

    /**
     * 计算临期预警标志
     * 规则：生产日期 + 保质期天数 = 到期日期
     * - 距当前时间3天以内（含3天）为临期，isWarning = 1
     * - 超过当前时间为过期，isWarning = 2
     * - 其他为正常，isWarning = 0
     */
    private void setWarning(List<Goods> list) {
        if (list == null) return;
        for (Goods goods : list) {
            setWarning(goods);
        }
    }

    private void setWarning(Goods goods) {
        if (goods == null || goods.getExpireDays() == null || goods.getExpireDays() <= 0 || goods.getProduceDate() == null) {
            return;
        }
        long produceTime = goods.getProduceDate().getTime();
        long expireTime = produceTime + (long) goods.getExpireDays() * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        long diffDays = (expireTime - now) / (24 * 60 * 60 * 1000);

        if (diffDays < 0) {
            goods.setIsWarning(2);  // 过期
        } else if (diffDays <= 3) {
            goods.setIsWarning(1);  // 临期
        } else {
            goods.setIsWarning(0);  // 正常
        }
    }

}