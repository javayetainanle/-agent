package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Type;
import com.example.exception.CustomException;
import com.example.mapper.TypeMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TypeService {

    @Resource
    private TypeMapper typeMapper;

    public void add(Type type) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        typeMapper.insert(type);
    }

    public void deleteById(Integer id) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        typeMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        for (Integer id : ids) {
            typeMapper.deleteById(id);
        }
    }

    public void updateById(Type type) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        typeMapper.updateById(type);
    }

    public Type selectById(Integer id) {
        Type type = typeMapper.selectById(id);
        if (type == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return type;
    }

    public List<Type> selectAll(Type type) {
        return typeMapper.selectAll(type);
    }

    public PageInfo<Type> selectPage(Type type, Integer pageNum, Integer pageSize) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        PageHelper.startPage(pageNum, pageSize);
        List<Type> list = typeMapper.selectAll(type);
        return PageInfo.of(list);
    }
}
