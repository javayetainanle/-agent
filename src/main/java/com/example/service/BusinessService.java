package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.enums.StatusEnum;
import com.example.entity.Account;
import com.example.entity.Business;
import com.example.exception.CustomException;
import com.example.mapper.BusinessMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BusinessService {

    @Resource
    private BusinessMapper businessMapper;

    public void add(Business business) {
        Account currentUser = TokenUtils.getCurrentUserOrNull();
        if (currentUser != null && !TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }

        Business dbBusiness = businessMapper.selectByUsername(business.getUsername());
        if (ObjectUtil.isNotNull(dbBusiness)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(business.getPassword())) {
            business.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        if (ObjectUtil.isEmpty(business.getName())) {
            business.setName(business.getUsername());
        }
        if (ObjectUtil.isEmpty(business.getStatus())) {
            business.setStatus(StatusEnum.CHECKING.status);
        }
        business.setRole(RoleEnum.BUSINESS.name());
        businessMapper.insert(business);
    }

    public void deleteById(Integer id) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        businessMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        for (Integer id : ids) {
            businessMapper.deleteById(id);
        }
    }

    public void updateById(Business business) {
        if (business.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Business existing = requireAccessibleBusiness(business.getId(), true);
        Account currentUser = TokenUtils.getCurrentUser();
        if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            business.setUsername(existing.getUsername());
            business.setPassword(existing.getPassword());
            business.setRole(existing.getRole());
            business.setStatus(existing.getStatus());
        }
        businessMapper.updateById(business);
    }

    public Business selectById(Integer id) {
        Business business = businessMapper.selectById(id);
        if (business == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return business;
    }

    public List<Business> selectAll(Business business) {
        return businessMapper.selectAll(business);
    }

    public PageInfo<Business> selectPage(Business business, Integer pageNum, Integer pageSize) {
        Business query = business != null ? business : new Business();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)) {
            query.setId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Business> list = businessMapper.selectAll(query);
        return PageInfo.of(list);
    }

    public Account login(Account account) {
        Account dbBusiness = businessMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbBusiness)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPassword().equals(dbBusiness.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        String tokenData = dbBusiness.getId() + "-" + RoleEnum.BUSINESS.name();
        String token = TokenUtils.createToken(tokenData, dbBusiness.getPassword());
        dbBusiness.setToken(token);
        return dbBusiness;
    }

    public void register(Account account) {
        Business business = new Business();
        BeanUtils.copyProperties(account, business);
        add(business);
    }

    public void updatePassword(Account account) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)
                || !Objects.equals(currentUser.getUsername(), account.getUsername())) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }

        Business dbBusiness = businessMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbBusiness)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!account.getPassword().equals(dbBusiness.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbBusiness.setPassword(account.getNewPassword());
        businessMapper.updateById(dbBusiness);
    }

    private Business requireAccessibleBusiness(Integer id, boolean allowOwner) {
        Business business = businessMapper.selectById(id);
        if (business == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return business;
        }
        if (allowOwner && TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)
                && Objects.equals(currentUser.getId(), business.getId())) {
            return business;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }
}
