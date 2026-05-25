package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Collect;
import com.example.exception.CustomException;
import com.example.mapper.CollectMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CollectService {

    @Resource
    private CollectMapper collectMapper;

    public void add(Collect collect) {
        Account currentUser = TokenUtils.getCurrentUser();
        TokenUtils.requireRole(RoleEnum.USER);
        if (collect.getGoodsId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        collect.setUserId(currentUser.getId());
        Collect dbCollect = collectMapper.selectByUserIdAndGoodsId(currentUser.getId(), collect.getGoodsId());
        if (ObjectUtil.isNotEmpty(dbCollect)) {
            throw new CustomException(ResultCodeEnum.COLLECT_ALREADY_ERROR);
        }
        collectMapper.insert(collect);
    }

    public void deleteById(Integer id) {
        Collect collect = requireAccessibleCollect(id);
        collectMapper.deleteById(collect.getId());
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(Collect collect) {
        if (collect.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Collect existing = requireAccessibleCollect(collect.getId());
        if (!TokenUtils.hasRole(TokenUtils.getCurrentUser(), RoleEnum.ADMIN)) {
            collect.setUserId(existing.getUserId());
            collect.setBusinessId(existing.getBusinessId());
            collect.setGoodsId(existing.getGoodsId());
        }
        collectMapper.updateById(collect);
    }

    public Collect selectById(Integer id) {
        return requireAccessibleCollect(id);
    }

    public List<Collect> selectAll(Collect collect) {
        Collect query = collect != null ? collect : new Collect();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            query.setUserId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        return collectMapper.selectAll(query);
    }

    public PageInfo<Collect> selectPage(Collect collect, Integer pageNum, Integer pageSize) {
        Collect query = collect != null ? collect : new Collect();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            query.setUserId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Collect> list = collectMapper.selectAll(query);
        return PageInfo.of(list);
    }

    private Collect requireAccessibleCollect(Integer id) {
        Collect collect = collectMapper.selectById(id);
        if (collect == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return collect;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER) && Objects.equals(collect.getUserId(), currentUser.getId())) {
            return collect;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }
}
