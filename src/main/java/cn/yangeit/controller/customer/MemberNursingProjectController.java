package cn.yangeit.controller.customer;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.yangeit.common.AjaxResult;
import cn.yangeit.common.TableDataInfo;
import cn.yangeit.config.BaseContext;
import cn.yangeit.dto.CheckOrderDto;
import cn.yangeit.dto.ReFundOrderDto;
import cn.yangeit.mapper.ElderMapper;
import cn.yangeit.mapper.NursingProjectMapper;
import cn.yangeit.mapper.OrderMapper;
import cn.yangeit.pojo.Elder;
import cn.yangeit.pojo.NursingProject;
import cn.yangeit.pojo.Order;
import cn.yangeit.vo.OrderVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 微信端用户端-项目相关接口
 */
@RestController
@RequestMapping("/customer/orders")
@Tag(name = "用户端-项目和订单接口",description = "项目接口")
public class MemberNursingProjectController  {

    @Autowired
    NursingProjectMapper nursingProjectMapper;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    ElderMapper elderMapper;


    @RequestMapping("project/page")
    @Operation(summary = "分页查询接口",description = "分页查询项目接口，返回项目列表")
    public TableDataInfo getByPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                   @RequestParam(value = "pageSize", defaultValue = "10")Integer pageSize) {
        Page<NursingProject> pages = new Page<>(pageNum, pageSize);
        Page<NursingProject> page = nursingProjectMapper.selectPage(pages, null);

        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("请求成功");
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotal());
        return rspData;
    }
    @GetMapping("project/{id}")
    @Operation(summary = "查询项目接口",description = "查询项目接口，返回项目信息")
    public AjaxResult getById(@PathVariable("id") Long id) {
        return AjaxResult.success(nursingProjectMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "添加接口",description = "添加项目接口，需要提供项目信息，返回添加结果")
    public AjaxResult addOrder(@RequestBody CheckOrderDto dto) {
        Long userId = BaseContext.getCurrentId();
        Order order = Order.builder()
                .memberId(userId)
                .projectId(dto.getProjectId())
                .elderId(dto.getElderId())
                .estimatedArrivalTime(dto.getEstimatedArrivalTime())
                .amount(BigDecimal.valueOf(dto.getAmount()))
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .createBy(userId)
                .updateBy(userId)
                .oCreateType(1)
                .remark(dto.getRemark())
                .orderNo(UUID.randomUUID().toString())
                .build();
        orderMapper.insert(order);
        return AjaxResult.success("下单成功");
    }

    @RequestMapping("/order/page")
    @Operation(summary = "分页查询接口",description = "分页查询项目接口，返回项目列表")
    public AjaxResult listByPage(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = BaseContext.getCurrentId();
        Page<Order> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Order> Wrapper = new QueryWrapper<>();
        Wrapper.eq("member_id",userId);
        Wrapper.eq(ObjectUtil.isNotEmpty(status),"status",status);
        Page<Order> orders = orderMapper.selectPage(page, Wrapper);

        List<OrderVo> collect = orders.getRecords().stream().map(order -> {
            OrderVo orderVO = OrderVo.builder().build();

            BeanUtil.copyProperties(order, orderVO);
            NursingProject nursingProject = nursingProjectMapper.selectById(order.getProjectId());
            orderVO.setImage(nursingProject.getImage());
            orderVO.setServiceName(nursingProject.getName());
            orderVO.setCreateTime(order.getEstimatedArrivalTime());

            Elder elder = elderMapper.selectById(order.getElderId());
            orderVO.setUserName(elder.getName());
            return orderVO;
        }).collect(Collectors.toList());

        return AjaxResult.success(collect);
    }

    @PostMapping("/refund")
    @Operation(summary = "退款接口",description = "退款接口，返回退款结果")
    public AjaxResult refund(@RequestBody @Validated ReFundOrderDto dto) {
        Order order = orderMapper.selectById(dto.getProductOrderNo());
        if(ObjectUtil.isEmpty(order)){
            return AjaxResult.error("订单不存在");
        }
        if(order.getStatus()!=1){
            return AjaxResult.error("订单错误");
        }
        order.setStatus(5);
        order.setReason(dto.getTradingChannel());
        order.setRefund(order.getAmount());
        order.setUpdateBy(BaseContext.getCurrentId());
        order.setUpdateTime(LocalDateTime.now());
        order.setIsRefund("YES");
        order.setOCreateType(1);
        orderMapper.updateById(order);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除接口",description = "删除项目接口，返回删除结果")
    public AjaxResult delete(@PathVariable("id") Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return AjaxResult.error("订单不存在");
        }
        orderMapper.deleteById(id);
        return AjaxResult.success();
    }
}
