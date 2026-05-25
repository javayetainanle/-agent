package cn.yangeit.config;

import com.example.entity.Account;
import com.example.entity.Address;
import com.example.entity.Cart;
import com.example.entity.Goods;
import com.example.entity.Orders;
import com.example.entity.User;
import com.example.mapper.AddressMapper;
import com.example.mapper.CartMapper;
import com.example.mapper.GoodsMapper;
import com.example.mapper.OrdersMapper;
import com.example.mapper.UserMapper;
import com.example.utils.TokenUtils;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ReservTools {

    private final OrdersMapper ordersMapper;
    private final CartMapper cartMapper;
    private final AddressMapper addressMapper;
    private final GoodsMapper goodsMapper;
    private final UserMapper userMapper;

    public ReservTools(OrdersMapper ordersMapper,
                       CartMapper cartMapper,
                       AddressMapper addressMapper,
                       GoodsMapper goodsMapper,
                       UserMapper userMapper) {
        this.ordersMapper = ordersMapper;
        this.cartMapper = cartMapper;
        this.addressMapper = addressMapper;
        this.goodsMapper = goodsMapper;
        this.userMapper = userMapper;
    }

    public record UserIdParam(String userId) {
    }

    public record GoodsNameParam(String name) {
    }

    public record UpdateCartNumParam(Integer cartId, Integer num) {
    }

    public record DeleteCartParam(Integer cartId) {
    }

    public record AddAddressParam(String userId, String username, String phone, String useraddress) {
    }

    public record DeleteAddressParam(Integer addressId) {
    }

    public record SubmitOrderParam(String userId, Integer addressId) {
    }

    public record AddToCartParam(String userId, Integer goodsId, Integer num) {
    }

    private Integer currentUserId() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            return currentId.intValue();
        }

        Account currentUser = TokenUtils.getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    private Integer resolveUserId(String userId) {
        Integer authenticatedUserId = currentUserId();
        if (authenticatedUserId != null) {
            return authenticatedUserId;
        }
        return parseUserId(userId);
    }

    private Integer parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(userId);
        }
        catch (NumberFormatException e) {
            User user = userMapper.selectByUsername(userId);
            return user != null ? user.getId() : null;
        }
    }

    private boolean isCurrentUserResource(Integer ownerUserId) {
        Integer authenticatedUserId = currentUserId();
        return authenticatedUserId == null || Objects.equals(authenticatedUserId, ownerUserId);
    }

    @Bean
    @Description("查询当前登录用户的订单列表；如果没有登录态，才会退化为使用 userId 参数。")
    public Function<UserIdParam, String> getOrderList() {
        return request -> {
            Integer userId = resolveUserId(request.userId());
            if (userId == null) {
                return "未找到有效的用户信息。";
            }

            Orders query = new Orders();
            query.setUserId(userId);
            List<Orders> orders = ordersMapper.selectAll(query);
            if (orders.isEmpty()) {
                return "当前没有查询到订单。";
            }

            return orders.stream()
                    .map(order -> String.format(
                            "订单号：%s，商品：%s，数量：%s，金额：%.2f元，状态：%s",
                            order.getOrderId(),
                            order.getGoodsName(),
                            order.getNum(),
                            order.getPrice(),
                            order.getStatus()))
                    .collect(Collectors.joining("\n"));
        };
    }

    @Bean
    @Description("查询当前登录用户的购物车列表；如果没有登录态，才会退化为使用 userId 参数。")
    public Function<UserIdParam, String> getCartList() {
        return request -> {
            Integer userId = resolveUserId(request.userId());
            if (userId == null) {
                return "未找到有效的用户信息。";
            }

            Cart query = new Cart();
            query.setUserId(userId);
            List<Cart> cartList = cartMapper.selectAll(query);
            if (cartList.isEmpty()) {
                return "当前购物车是空的。";
            }

            return cartList.stream()
                    .map(cart -> String.format(
                            "购物车ID：%s，商品：%s，数量：%s，单价：%.2f元",
                            cart.getId(),
                            cart.getGoodsName(),
                            cart.getNum(),
                            cart.getGoodsPrice()))
                    .collect(Collectors.joining("\n"));
        };
    }

    @Bean
    @Description("查询当前登录用户的收货地址列表；如果没有登录态，才会退化为使用 userId 参数。")
    public Function<UserIdParam, String> getAddressList() {
        return request -> {
            Integer userId = resolveUserId(request.userId());
            if (userId == null) {
                return "未找到有效的用户信息。";
            }

            Address query = new Address();
            query.setUserId(userId);
            List<Address> addresses = addressMapper.selectAll(query);
            if (addresses.isEmpty()) {
                return "当前还没有收货地址。";
            }

            return addresses.stream()
                    .map(address -> String.format(
                            "地址ID：%s，收货人：%s，电话：%s，地址：%s",
                            address.getId(),
                            address.getUsername(),
                            address.getPhone(),
                            address.getUseraddress()))
                    .collect(Collectors.joining("\n"));
        };
    }

    @Bean
    @Description("根据商品名称搜索商品。")
    public Function<GoodsNameParam, String> searchGoods() {
        return request -> {
            if (request.name() == null || request.name().isBlank()) {
                return "请提供要搜索的商品名称。";
            }

            List<Goods> goodsList = goodsMapper.selectByName(request.name());
            if (goodsList.isEmpty()) {
                return "没有找到相关商品。";
            }

            return goodsList.stream()
                    .map(goods -> String.format(
                            "商品ID：%s，商品：%s，价格：%.2f元/%s",
                            goods.getId(),
                            goods.getName(),
                            goods.getPrice(),
                            goods.getUnit()))
                    .collect(Collectors.joining("\n"));
        };
    }

    @Bean
    @Description("获取热销商品 Top5。")
    public Function<UserIdParam, String> getTop5Goods() {
        return ignored -> {
            List<Goods> goodsList = goodsMapper.selectTop5();
            if (goodsList.isEmpty()) {
                return "当前没有热销商品数据。";
            }

            return goodsList.stream()
                    .map(goods -> String.format(
                            "商品ID：%s，商品：%s，价格：%.2f元/%s",
                            goods.getId(),
                            goods.getName(),
                            goods.getPrice(),
                            goods.getUnit()))
                    .collect(Collectors.joining("\n"));
        };
    }

    @Bean
    @Description("将商品加入当前登录用户的购物车；登录态存在时会忽略 userId 参数。")
    public Function<AddToCartParam, String> addToCart() {
        return request -> {
            Integer userId = resolveUserId(request.userId());
            if (userId == null) {
                return "未找到有效的用户信息。";
            }
            if (request.goodsId() == null) {
                return "请提供商品ID。";
            }

            Goods goods = goodsMapper.selectById(request.goodsId());
            if (goods == null) {
                return "商品不存在。";
            }

            int count = request.num() != null && request.num() > 0 ? request.num() : 1;
            Cart existing = cartMapper.selectByUserIdAndGoodsId(userId, request.goodsId());
            if (existing != null) {
                existing.setNum(existing.getNum() + count);
                cartMapper.updateById(existing);
                return String.format("商品已在购物车中，已追加 %s 件，当前数量为 %s。", count, existing.getNum());
            }

            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setBusinessId(goods.getBusinessId());
            cart.setGoodsId(goods.getId());
            cart.setNum(count);
            cartMapper.insert(cart);
            return String.format("已将商品【%s】加入购物车，数量：%s。", goods.getName(), count);
        };
    }

    @Bean
    @Description("修改当前登录用户购物车中某个商品的数量。")
    public Function<UpdateCartNumParam, String> updateCartNum() {
        return request -> {
            if (request.cartId() == null || request.num() == null) {
                return "请同时提供 cartId 和 num。";
            }
            if (request.num() <= 0) {
                return "商品数量必须大于 0。";
            }

            Cart cart = cartMapper.selectById(request.cartId());
            if (cart == null) {
                return "购物车记录不存在。";
            }
            if (!isCurrentUserResource(cart.getUserId())) {
                return "无权修改这条购物车记录。";
            }

            cart.setNum(request.num());
            cartMapper.updateById(cart);
            return String.format("已将购物车商品数量修改为 %s。", request.num());
        };
    }

    @Bean
    @Description("删除当前登录用户购物车中的某个商品。")
    public Function<DeleteCartParam, String> deleteCart() {
        return request -> {
            if (request.cartId() == null) {
                return "请提供 cartId。";
            }

            Cart cart = cartMapper.selectById(request.cartId());
            if (cart == null) {
                return "购物车记录不存在。";
            }
            if (!isCurrentUserResource(cart.getUserId())) {
                return "无权删除这条购物车记录。";
            }

            cartMapper.deleteById(request.cartId());
            return String.format("已删除购物车商品【%s】。", cart.getGoodsName());
        };
    }

    @Bean
    @Description("为当前登录用户新增收货地址；登录态存在时会忽略 userId 参数。")
    public Function<AddAddressParam, String> addAddress() {
        return request -> {
            Integer userId = resolveUserId(request.userId());
            if (userId == null) {
                return "未找到有效的用户信息。";
            }
            if (request.username() == null || request.username().isBlank()
                    || request.phone() == null || request.phone().isBlank()
                    || request.useraddress() == null || request.useraddress().isBlank()) {
                return "请提供完整的收货人、电话和收货地址信息。";
            }

            Address address = new Address();
            address.setUserId(userId);
            address.setUsername(request.username());
            address.setPhone(request.phone());
            address.setUseraddress(request.useraddress());
            addressMapper.insert(address);

            return String.format("已添加收货地址：%s，%s，%s。", request.username(), request.phone(), request.useraddress());
        };
    }

    @Bean
    @Description("删除当前登录用户的收货地址。")
    public Function<DeleteAddressParam, String> deleteAddress() {
        return request -> {
            if (request.addressId() == null) {
                return "请提供 addressId。";
            }

            Address address = addressMapper.selectById(request.addressId());
            if (address == null) {
                return "收货地址不存在。";
            }
            if (!isCurrentUserResource(address.getUserId())) {
                return "无权删除这个收货地址。";
            }

            addressMapper.deleteById(request.addressId());
            return String.format("已删除收货地址：%s。", address.getUseraddress());
        };
    }

    @Bean
    @Description("根据当前登录用户的购物车提交订单；登录态存在时会忽略 userId 参数。")
    public Function<SubmitOrderParam, String> submitOrder() {
        return request -> {
            Integer userId = resolveUserId(request.userId());
            if (userId == null) {
                return "未找到有效的用户信息。";
            }
            if (request.addressId() == null) {
                return "请提供收货地址 ID。";
            }

            Address address = addressMapper.selectById(request.addressId());
            if (address == null) {
                return "收货地址不存在。";
            }
            if (!isCurrentUserResource(address.getUserId())) {
                return "无权使用这个收货地址下单。";
            }

            Cart query = new Cart();
            query.setUserId(userId);
            List<Cart> cartList = cartMapper.selectAll(query);
            if (cartList.isEmpty()) {
                return "购物车为空，无法提交订单。";
            }

            String orderPrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            StringBuilder builder = new StringBuilder("订单提交成功：\n");
            int index = 0;
            for (Cart cart : cartList) {
                Orders order = new Orders();
                order.setOrderId(orderPrefix + String.format("%03d", ++index));
                order.setUserId(userId);
                order.setBusinessId(cart.getBusinessId());
                order.setGoodsId(cart.getGoodsId());
                order.setAddressId(request.addressId());
                order.setNum(cart.getNum());
                order.setPrice(cart.getGoodsPrice() * cart.getNum());
                order.setStatus("待支付");
                order.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                ordersMapper.insert(order);
                cartMapper.deleteById(cart.getId());

                builder.append(String.format(
                        "订单号：%s，商品：%s，数量：%s，金额：%.2f元%n",
                        order.getOrderId(),
                        cart.getGoodsName(),
                        cart.getNum(),
                        order.getPrice()));
            }
            return builder.toString().trim();
        };
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }
}
