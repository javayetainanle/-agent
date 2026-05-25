package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Notice;
import com.example.exception.CustomException;
import com.example.mapper.NoticeMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    public void add(Notice notice) {
        TokenUtils.requireRole(RoleEnum.ADMIN, RoleEnum.BUSINESS);
        Account currentUser = TokenUtils.getCurrentUser();
        notice.setTime(DateUtil.today());
        notice.setUser(currentUser.getUsername());
        noticeMapper.insert(notice);
    }

    public void deleteById(Integer id) {
        TokenUtils.requireRole(RoleEnum.ADMIN, RoleEnum.BUSINESS);
        noticeMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        TokenUtils.requireRole(RoleEnum.ADMIN, RoleEnum.BUSINESS);
        for (Integer id : ids) {
            noticeMapper.deleteById(id);
        }
    }

    public void updateById(Notice notice) {
        TokenUtils.requireRole(RoleEnum.ADMIN, RoleEnum.BUSINESS);
        noticeMapper.updateById(notice);
    }

    public Notice selectById(Integer id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return notice;
    }

    public List<Notice> selectAll(Notice notice) {
        return noticeMapper.selectAll(notice);
    }

    public PageInfo<Notice> selectPage(Notice notice, Integer pageNum, Integer pageSize) {
        TokenUtils.requireRole(RoleEnum.ADMIN, RoleEnum.BUSINESS);
        PageHelper.startPage(pageNum, pageSize);
        List<Notice> list = noticeMapper.selectAll(notice);
        return PageInfo.of(list);
    }
}
