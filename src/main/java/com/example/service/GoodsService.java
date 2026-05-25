package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Cart;
import com.example.entity.Collect;
import com.example.entity.Goods;
import com.example.entity.Orders;
import com.example.exception.CustomException;
import com.example.mapper.CartMapper;
import com.example.mapper.CollectMapper;
import com.example.mapper.GoodsMapper;
import com.example.mapper.OrdersMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    private CollectMapper collectMapper;
    @Resource
    private CartMapper cartMapper;
    @Resource
    private OrdersMapper ordersMapper;

    public void add(Goods goods) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)) {
            goods.setBusinessId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        goodsMapper.insert(goods);
    }

    public void deleteById(Integer id) {
        Goods goods = requireManageableGoods(id);
        goodsMapper.deleteById(goods.getId());
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(Goods goods) {
        if (goods.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Goods existing = requireManageableGoods(goods.getId());
        if (!TokenUtils.hasRole(TokenUtils.getCurrentUser(), RoleEnum.ADMIN)) {
            goods.setBusinessId(existing.getBusinessId());
        }
        goodsMapper.updateById(goods);
    }

    public Goods selectById(Integer id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return goods;
    }

    public List<Goods> selectAll(Goods goods) {
        return goodsMapper.selectAll(goods);
    }

    public PageInfo<Goods> selectPage(Goods goods, Integer pageNum, Integer pageSize) {
        Goods query = goods != null ? goods : new Goods();
        Account currentUser = TokenUtils.getCurrentUserOrNull();
        if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)) {
            query.setBusinessId(currentUser.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Goods> list = goodsMapper.selectAll(query);
        return PageInfo.of(list);
    }

    public List<Goods> selectTop5() {
        return goodsMapper.selectTop5();
    }

    public List<Goods> selectByTypeId(Integer id) {
        return goodsMapper.selectByTypeId(id);
    }

    public List<Goods> selectByBusinessId(Integer id) {
        return goodsMapper.selectByBusinessId(id);
    }

    public List<Goods> selectByName(String name) {
        return goodsMapper.selectByName(name);
    }

    public List<Goods> recommend() {
        Account currentUser = TokenUtils.getCurrentUser();
        TokenUtils.requireRole(RoleEnum.USER);

        Integer userId = currentUser.getId();
        List<Goods> allGoods = goodsMapper.selectAll(null);
        if (allGoods.isEmpty()) {
            return new ArrayList<>();
        }

        Collect collectQuery = new Collect();
        collectQuery.setUserId(userId);
        List<Collect> myCollects = collectMapper.selectAll(collectQuery);

        Cart cartQuery = new Cart();
        cartQuery.setUserId(userId);
        List<Cart> myCarts = cartMapper.selectAll(cartQuery);

        Orders orderQuery = new Orders();
        orderQuery.setUserId(userId);
        List<Orders> myOrders = ordersMapper.selectAll(orderQuery);

        Set<Integer> interactedGoodsIds = new LinkedHashSet<>();
        myOrders.forEach(order -> interactedGoodsIds.add(order.getGoodsId()));
        myCarts.forEach(cart -> interactedGoodsIds.add(cart.getGoodsId()));
        myCollects.forEach(collect -> interactedGoodsIds.add(collect.getGoodsId()));

        if (interactedGoodsIds.isEmpty()) {
            return getRandomGoods(5);
        }

        List<Goods> recommendResult = new ArrayList<>();
        List<Goods> interactedGoods = allGoods.stream()
                .filter(goods -> interactedGoodsIds.contains(goods.getId()))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(3, interactedGoods.size()); i++) {
            recommendResult.add(interactedGoods.get(i));
        }

        Set<Integer> businessIds = interactedGoods.stream()
                .map(Goods::getBusinessId)
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toSet());

        List<Goods> sameBusinessGoods = allGoods.stream()
                .filter(goods -> !interactedGoodsIds.contains(goods.getId()))
                .filter(goods -> goods.getBusinessId() != null && businessIds.contains(goods.getBusinessId()))
                .collect(Collectors.toList());
        Collections.shuffle(sameBusinessGoods);
        for (int i = 0; i < Math.min(2, sameBusinessGoods.size()); i++) {
            recommendResult.add(sameBusinessGoods.get(i));
        }

        if (recommendResult.size() < 5) {
            int remain = 5 - recommendResult.size();
            List<Goods> randomList = getRandomGoods(remain);
            Set<Integer> existingIds = recommendResult.stream()
                    .map(Goods::getId)
                    .collect(Collectors.toSet());
            for (Goods goods : randomList) {
                if (!existingIds.contains(goods.getId())) {
                    recommendResult.add(goods);
                    existingIds.add(goods.getId());
                    if (recommendResult.size() >= 5) {
                        break;
                    }
                }
            }
        }
        return recommendResult;
    }

    private Goods requireManageableGoods(Integer id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return goods;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)
                && goods.getBusinessId() != null
                && goods.getBusinessId().equals(currentUser.getId())) {
            return goods;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }

    private List<Goods> getRandomGoods(int num) {
        List<Goods> goods = goodsMapper.selectAll(null);
        if (goods.isEmpty() || num <= 0) {
            return new ArrayList<>();
        }
        List<Goods> result = new ArrayList<>(num);
        Random random = new Random();
        for (int i = 0; i < num; i++) {
            result.add(goods.get(random.nextInt(goods.size())));
        }
        return result;
    }
}
