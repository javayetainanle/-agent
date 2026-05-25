package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Address;
import com.example.entity.Cart;
import com.example.entity.Goods;
import com.example.entity.Orders;
import com.example.exception.CustomException;
import com.example.mapper.AddressMapper;
import com.example.mapper.CartMapper;
import com.example.mapper.GoodsMapper;
import com.example.mapper.OrdersMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class OrdersService {

    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private CartMapper cartMapper;
    @Resource
    private GoodsMapper goodsMapper;
    @Resource
    private AddressMapper addressMapper;

    @Transactional(rollbackFor = Exception.class)
    public void add(Orders orders) {
        Account currentUser = TokenUtils.getCurrentUser();
        TokenUtils.requireRole(RoleEnum.USER);

        if (orders.getAddressId() == null || orders.getCartData() == null || orders.getCartData().isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Address address = addressMapper.selectById(orders.getAddressId());
        if (address == null || !Objects.equals(address.getUserId(), currentUser.getId())) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }

        String orderPrefix = DateUtil.format(orders.getTime() == null ? new java.util.Date() : DateUtil.parse(orders.getTime()), "yyyyMMddHHmmss");
        int index = 0;
        for (Cart clientCart : orders.getCartData()) {
            Cart cart = loadCurrentUserCart(clientCart.getId(), currentUser.getId());
            Goods goods = goodsMapper.selectById(cart.getGoodsId());
            if (goods == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR);
            }

            Orders newOrder = new Orders();
            newOrder.setOrderId(orderPrefix + String.format("%03d", ++index));
            newOrder.setUserId(currentUser.getId());
            newOrder.setBusinessId(cart.getBusinessId());
            newOrder.setGoodsId(cart.getGoodsId());
            newOrder.setAddressId(address.getId());
            newOrder.setNum(cart.getNum());
            newOrder.setPrice(cart.getGoodsPrice() * cart.getNum());
            newOrder.setStatus("待支付");
            newOrder.setTime(DateUtil.now());
            ordersMapper.insert(newOrder);

            goods.setCount(goods.getCount() + cart.getNum());
            goodsMapper.updateById(goods);
            cartMapper.deleteById(cart.getId());
        }
    }

    public void deleteById(Integer id) {
        Orders order = requireAccessibleOrder(id);
        ordersMapper.deleteById(order.getId());
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    public void updateById(Orders orders) {
        if (orders.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Orders existing = requireAccessibleOrder(orders.getId());
        Account currentUser = TokenUtils.getCurrentUser();
        if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            orders.setUserId(existing.getUserId());
            orders.setBusinessId(existing.getBusinessId());
            orders.setGoodsId(existing.getGoodsId());
            orders.setAddressId(existing.getAddressId());
            orders.setOrderId(existing.getOrderId());
            orders.setNum(existing.getNum());
            orders.setPrice(existing.getPrice());
            orders.setTime(existing.getTime());
        }
        ordersMapper.updateById(orders);
    }

    public Orders selectById(Integer id) {
        return requireAccessibleOrder(id);
    }

    public List<Orders> selectAll(Orders orders) {
        Orders query = orders != null ? orders : new Orders();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            query.setUserId(currentUser.getId());
        } else if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)) {
            query.setBusinessId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        return ordersMapper.selectAll(query);
    }

    public PageInfo<Orders> selectPage(Orders orders, Integer pageNum, Integer pageSize) {
        Orders query = orders != null ? orders : new Orders();
        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER)) {
            query.setUserId(currentUser.getId());
        } else if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS)) {
            query.setBusinessId(currentUser.getId());
        } else if (!TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = ordersMapper.selectAll(query);
        return PageInfo.of(list);
    }

    private Orders requireAccessibleOrder(Integer id) {
        Orders order = ordersMapper.selectById(id);
        if (order == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }

        Account currentUser = TokenUtils.getCurrentUser();
        if (TokenUtils.hasRole(currentUser, RoleEnum.ADMIN)) {
            return order;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.USER) && Objects.equals(order.getUserId(), currentUser.getId())) {
            return order;
        }
        if (TokenUtils.hasRole(currentUser, RoleEnum.BUSINESS) && Objects.equals(order.getBusinessId(), currentUser.getId())) {
            return order;
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }

    private Cart loadCurrentUserCart(Integer cartId, Integer currentUserId) {
        if (cartId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Cart query = new Cart();
        query.setId(cartId);
        query.setUserId(currentUserId);
        List<Cart> carts = cartMapper.selectAll(query);
        if (carts == null || carts.isEmpty()) {
            throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
        }
        return carts.get(0);
    }
}
