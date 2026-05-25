package cn.yangeit.service.impl;

import cn.yangeit.mapper.OrderMapper;
import cn.yangeit.pojo.Order;
import cn.yangeit.service.IOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * Legacy order service implementation retained for compatibility with the
 * historical customer module.
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {
}
