package cn.yangeit.service;

import cn.yangeit.pojo.Order;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Legacy order service abstraction retained for compatibility with the
 * historical customer module.
 */
public interface IOrderService extends IService<Order> {
}
