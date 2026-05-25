package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Address;
import com.example.exception.CustomException;
import com.example.mapper.AddressMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AddressService {

    @Resource
    private AddressMapper addressMapper;

    public void add(Address address) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!TokenUtils.hasRole(currentUser, RoleEnum.USER) && !TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            address.setUserId(currentUser.getId());
        }
        addressMapper.insert(address);
    }

    public void deleteById(Integer id) {
        Address address = requireAccessibleAddress(id);
        addressMapper.deleteById(address.getId());
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(Address address) {
        if (address.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Address existing = requireAccessibleAddress(address.getId());
        if (!isAdmin()) {
            address.setUserId(existing.getUserId());
        }
        addressMapper.updateById(address);
    }

    public Address selectById(Integer id) {
        return requireAccessibleAddress(id);
    }

    public List<Address> selectAll(Address address) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            address.setUserId(currentUser.getId());
        }
        else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        return addressMapper.selectAll(address);
    }

    public PageInfo<Address> selectPage(Address address, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            address.setUserId(currentUser.getId());
        }
        else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Address> list = addressMapper.selectAll(address);
        return PageInfo.of(list);
    }

    private Address requireAccessibleAddress(Integer id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return address;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER) && Objects.equals(address.getUserId(), currentUser.getId())) {
            return address;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }

    private boolean isAdmin() {
        return TokenUtils.hasRole(TokenUtils.getCurrentUser(), RoleEnum.ADMIN);
    }
}
