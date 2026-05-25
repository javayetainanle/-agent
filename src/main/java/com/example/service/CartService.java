package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Cart;
import com.example.entity.Goods;
import com.example.exception.CustomException;
import com.example.mapper.CartMapper;
import com.example.mapper.GoodsMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CartService {

    @Resource
    private CartMapper cartMapper;
    @Resource
    private GoodsMapper goodsMapper;

    public void add(Cart cart) {
        Account currentUser = TokenUtils.getCurrentUser();
        TokenUtils.requireRole(RoleEnum.USER);
        if (cart.getGoodsId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Goods goods = goodsMapper.selectById(cart.getGoodsId());
        if (goods == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        cart.setUserId(currentUser.getId());
        cart.setBusinessId(goods.getBusinessId());
        int num = cart.getNum() == null || cart.getNum() <= 0 ? 1 : cart.getNum();
        cart.setNum(num);

        Cart dbCart = cartMapper.selectByUserIdAndGoodsId(currentUser.getId(), cart.getGoodsId());
        if (ObjectUtil.isNotEmpty(dbCart)) {
            dbCart.setNum(dbCart.getNum() + num);
            cartMapper.updateById(dbCart);
        } else {
            cartMapper.insert(cart);
        }
    }

    public void deleteById(Integer id) {
        Cart cart = requireAccessibleCart(id);
        cartMapper.deleteById(cart.getId());
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(Cart cart) {
        if (cart.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Cart existing = requireAccessibleCart(cart.getId());
        if (!TokenUtils.hasRole(TokenUtils.getCurrentUser(), RoleEnum.ADMIN)) {
            cart.setUserId(existing.getUserId());
            cart.setBusinessId(existing.getBusinessId());
            cart.setGoodsId(existing.getGoodsId());
        }
        cartMapper.updateById(cart);
    }

    public Cart selectById(Integer id) {
        return requireAccessibleCart(id);
    }

    public List<Cart> selectAll(Cart cart) {
        Cart query = cart != null ? cart : new Cart();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            query.setUserId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        return cartMapper.selectAll(query);
    }

    public PageInfo<Cart> selectPage(Cart cart, Integer pageNum, Integer pageSize) {
        Cart query = cart != null ? cart : new Cart();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            query.setUserId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Cart> list = cartMapper.selectAll(query);
        return PageInfo.of(list);
    }

    private Cart requireAccessibleCart(Integer id) {
        Cart cart = cartMapper.selectById(id);
        if (cart == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return cart;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER) && Objects.equals(cart.getUserId(), currentUser.getId())) {
            return cart;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }
}
