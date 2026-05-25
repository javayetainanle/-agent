package cn.yangeit.controller.customer;

import cn.yangeit.config.LoggingAdvisor;
import cn.yangeit.service.RagKnowledgeService;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.utils.TokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RestController
@CrossOrigin
@Tag(name = "用户端 AI 客服接口", description = "用户端 AI 客服接口")
public class OpenAiController {

    private final ChatClient chatClient;
    private final RagKnowledgeService ragKnowledgeService;

    public OpenAiController(ChatClient.Builder chatClientBuilder,
                            ChatMemory chatMemory,
                            RagKnowledgeService ragKnowledgeService) {
        this.ragKnowledgeService = ragKnowledgeService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        你是社区生鲜购物平台的专属智能客服，请使用中文，以友好、清晰、可靠的方式回答用户问题。
                        你正在通过在线聊天系统与用户互动，可以帮助用户完成以下事情：
                        1. 商品咨询：根据商品名称搜索商品，或者查看热销商品 Top5。
                        2. 订单查询：查询当前用户的订单列表。
                        3. 购物车管理：查看购物车、加入购物车、修改商品数量、删除购物车商品。
                        4. 收货地址管理：查看地址列表、添加收货地址、删除收货地址。
                        5. 提交订单：将购物车商品生成订单。

                        回复时请遵守这些规则：
                        1. 只能围绕社区生鲜购物业务回答，不要扩展到无关领域。
                        2. 涉及订单、购物车、地址、下单等操作时，请优先通过工具获取真实数据，不要自行编造。
                        3. 如果命中了知识库上下文，请优先参考知识库内容回答平台规则、订单状态说明、售后说明等问题。
                        4. 如果知识库没有命中，就基于已有业务规则和工具结果正常回答，不要硬编知识库内容。
                        5. 如果返回的是列表信息，请整理成用户容易理解的自然语言，不要原样堆砌字段。
                        6. 如果用户信息不足，请明确告诉用户还缺什么信息。

                        今天的日期是 {current_date}
                        当前用户 ID 是 {userId}
                        当前用户名是 {name}
                        本次检索到的知识库上下文如下：
                        {knowledge_context}
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new LoggingAdvisor()
                )
                .defaultToolNames(
                        "getOrderList",
                        "getCartList",
                        "getAddressList",
                        "searchGoods",
                        "getTop5Goods",
                        "addToCart",
                        "updateCartNum",
                        "deleteCart",
                        "addAddress",
                        "deleteAddress",
                        "submitOrder"
                )
                .build();
    }

    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "用户端 AI 客服接口", description = "用户端 AI 客服接口")
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "讲个笑话") String message) {

        Account currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Flux.just("请先登录后再使用 AI 客服。", "[complete]");
        }
        if (!RoleEnum.USER.name().equals(currentUser.getRole())) {
            return Flux.just("当前账号暂不支持使用 AI 客服。", "[complete]");
        }

        String conversationId = "user-" + currentUser.getId();
        String displayName = currentUser.getName() != null ? currentUser.getName() : currentUser.getUsername();
        final String finalDisplayName = (displayName == null || displayName.isBlank()) ? "用户" : displayName;
        final String knowledgeContext = ragKnowledgeService.buildContext(message);

        Flux<String> result = this.chatClient
                .prompt()
                .user(message)
                .system(promptSystemSpec -> {
                    promptSystemSpec.param("current_date", LocalDate.now().toString());
                    promptSystemSpec.param("userId", currentUser.getId());
                    promptSystemSpec.param("name", finalDisplayName);
                    promptSystemSpec.param("knowledge_context", knowledgeContext);
                })
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();

        return result.concatWith(Flux.just("[complete]"));
    }
}
