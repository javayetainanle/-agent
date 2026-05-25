package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Comment;
import com.example.exception.CustomException;
import com.example.mapper.CommentMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CommentService {

    @Resource
    private CommentMapper commentMapper;

    public void add(Comment comment) {
        Account currentUser = TokenUtils.getCurrentUser();
        TokenUtils.requireRole(RoleEnum.USER);
        comment.setUserId(currentUser.getId());
        comment.setTime(DateUtil.now());
        commentMapper.insert(comment);
    }

    public void deleteById(Integer id) {
        Comment comment = requireAccessibleComment(id);
        commentMapper.deleteById(comment.getId());
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(Comment comment) {
        if (comment.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Comment existing = requireAccessibleComment(comment.getId());
        if (!TokenUtils.hasRole(TokenUtils.getCurrentUser(), RoleEnum.ADMIN)) {
            comment.setUserId(existing.getUserId());
            comment.setBusinessId(existing.getBusinessId());
            comment.setGoodsId(existing.getGoodsId());
            comment.setTime(existing.getTime());
        }
        commentMapper.updateById(comment);
    }

    public Comment selectById(Integer id) {
        return requireAccessibleComment(id);
    }

    public List<Comment> selectAll(Comment comment) {
        TokenUtils.requireRole(RoleEnum.ADMIN);
        return commentMapper.selectAll(comment);
    }

    public PageInfo<Comment> selectPage(Comment comment, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            comment.setUserId(currentUser.getId());
        } else if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)) {
            comment.setBusinessId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> list = commentMapper.selectAll(comment);
        return PageInfo.of(list);
    }

    public List<Comment> selectByGoodsId(Integer id) {
        return commentMapper.selectByGoodsId(id);
    }

    private Comment requireAccessibleComment(Integer id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return comment;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER) && Objects.equals(comment.getUserId(), currentUser.getId())) {
            return comment;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS) && Objects.equals(comment.getBusinessId(), currentUser.getId())) {
            return comment;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }
}
