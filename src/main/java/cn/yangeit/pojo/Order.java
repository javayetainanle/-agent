package cn.yangeit.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Legacy order entity retained for compatibility with the historical customer
 * module.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("np_order")
@Builder
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tradingOrderNo;
    private Integer paymentStatus;
    private BigDecimal amount;
    private BigDecimal refund;
    private String isRefund;
    private Long memberId;
    private Long projectId;
    private Long elderId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedArrivalTime;

    private String mark;
    private String reason;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createBy;
    private Long updateBy;
    private String remark;
    private String viewStatus;
    private String orderNo;
    private Integer oCreateType;
}
