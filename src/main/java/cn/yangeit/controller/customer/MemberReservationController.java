package cn.yangeit.controller.customer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.yangeit.common.AjaxResult;
import cn.yangeit.common.BaseException;

import cn.yangeit.common.TableDataInfo;
import cn.yangeit.config.BaseContext;
import cn.yangeit.dto.ReservationDto;
import cn.yangeit.mapper.ReservationMapper;
import cn.yangeit.pojo.Reservation;
import cn.yangeit.vo.TimeCountVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约信息Controller
 */
@RestController
@RequestMapping("/customer/reservation")
@Tag(name = "用户端-预约模块",description = "预约接口")
public class MemberReservationController {

    @Autowired
    ReservationMapper reservationMapper;

    @GetMapping("cancelled-count")
    @Operation(summary = "获取用户取消的预约次数接口",description = "获取用户取消的预约次数接口")
    public AjaxResult getCancelledReservationCount(HttpServletRequest request){
        Long userId = BaseContext.getCurrentId();
        QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_by",userId);
        queryWrapper.eq("status",2);
        long count = reservationMapper.selectCount(queryWrapper);
        return AjaxResult.success(count);
    }

    @GetMapping("/countByTime")
    @Operation(summary = "统计指定时间段内预约次数接口",description = "统计指定时间段内预约次数接口")
    public AjaxResult countReservationsForEachTimeWithinTimeRange(Long time) {
        LocalDateTime localDateTime = LocalDateTimeUtil.of(time);
        //2024-09-21 00:00:00
        LocalDateTime startTime = localDateTime.toLocalDate().atStartOfDay();
        //2024-09-22 00:00:00
        LocalDateTime endTime = startTime.plusHours(24);
        List<TimeCountVo> timeCountVoList = reservationMapper.countReservationsForTime(startTime, endTime);

        return AjaxResult.success(timeCountVoList);
    }

    @PostMapping()
    @Operation(summary = "添加预约接口",description = "添加预约接口")
    public AjaxResult add(@RequestBody ReservationDto reservationDto, HttpServletRequest request){
        Long userId = BaseContext.getCurrentId();
        //Reservation reservation = new Reservation();
        //BeanUtils.copyProperties(reservation, reservationDto);
        Reservation reservation= BeanUtil.toBean(reservationDto, Reservation.class);

        reservation.setCreateBy(userId);
        reservation.setUpdateBy(userId);
        reservation.setCreateTime(LocalDateTime.now());
        reservation.setStatus(0);

        boolean result =reservationMapper.insert(reservation)>0?true:false;
        return AjaxResult.success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询接口",description = "分页查询预约接口")
    public AjaxResult finfByPage(Integer pageNum, Integer pageSize,Integer status, HttpServletRequest request){
        Long userId = BaseContext.getCurrentId();
        Page page = new Page<>(pageNum, pageSize);
        QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("create_by",userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty( status),"status",status);
        queryWrapper.orderByDesc("create_time");

        List list = reservationMapper.selectList(page, queryWrapper);

        TableDataInfo rspData = new TableDataInfo<>();
        rspData.setCode(200);
        rspData.setMsg("请求成功");
        rspData.setRows(list);
        rspData.setTotal(page.getTotal());

        return AjaxResult.success(rspData);
    }

    @PutMapping("{id}/cancel")
    @Operation(summary = "取消预约接口",description = "取消预约接口")
    public AjaxResult cancel(@PathVariable Long id, HttpServletRequest request){
        Long userId = BaseContext.getCurrentId();
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null){
            //return AjaxResult.error("预约信息不存在");
            throw new BaseException("预约信息不存在");
        }

        UpdateWrapper<Reservation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",id);
        updateWrapper.set("status",2);
        updateWrapper.set("update_by",userId);
        reservationMapper.update(updateWrapper);
        return AjaxResult.success();
    }
}