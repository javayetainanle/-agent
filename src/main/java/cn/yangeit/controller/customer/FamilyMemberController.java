package cn.yangeit.controller.customer;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.yangeit.common.AjaxResult;
import cn.yangeit.common.JwtUtils;
import cn.yangeit.config.BaseContext;
import cn.yangeit.dto.MemberElderDto;
import cn.yangeit.dto.UserLoginRequestDto;
import cn.yangeit.mapper.ElderMapper;
import cn.yangeit.mapper.FamilyMemberElderMapper;
import cn.yangeit.pojo.Elder;
import cn.yangeit.pojo.FamilyMemberElder;
import cn.yangeit.service.FamilyMemberService;
import cn.yangeit.vo.FamilyMemberElderVo;
import cn.yangeit.vo.LoginVo;
import cn.yangeit.vo.MemberElderVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.jsonwebtoken.Claims;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 微信端的用户相关接口
 */
@RestController
@RequestMapping("/customer/user")
@Tag(name = "用户接口",description = "家人登录，绑定接触家人等")
public class FamilyMemberController
{
    @Autowired
    private FamilyMemberService familyMemberService;
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    FamilyMemberElderMapper familyMemberElderMapper;

    @PostMapping("/login")
    @Operation(summary = "登录接口",description = "微信小程序登录的接口,需要提供授权码和手机号授权码等信息，返回token令牌")
    public AjaxResult login(@RequestBody UserLoginRequestDto userLoginRequestDto){
        LoginVo loginVo = familyMemberService.login(userLoginRequestDto);
        return AjaxResult.success(loginVo);
    }

    @PostMapping("/add")
    @Operation(summary = "添加接口",description = "添加家人接口，需要提供家人信息，返回添加结果")
    public AjaxResult add(@RequestBody MemberElderDto memberElderDto, HttpServletRequest request) {
        //1.通过身份证查询老人
        QueryWrapper<Elder> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("id_card_no", memberElderDto.getIdCard());
        Elder elder = elderMapper.selectOne(wrapper1);
        //2.判断老人是否存在
        if(ObjectUtil.isEmpty(elder)){
            return AjaxResult.error("没有该老人");
        }
        //3 获得发当前登陆人的userid
        String token = request.getHeader("authorization");
        Claims claims = JwtUtils.parseJWT(token);
        Long userId= Long.valueOf(claims.get("userId").toString());
        //4 通过userId和老人id查询是否已经绑定
        QueryWrapper<FamilyMemberElder> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("elder_id",elder.getId());
        queryWrapper2.eq("family_member_id",userId);
        long count = familyMemberElderMapper.selectCount(queryWrapper2);

        if(count > 0){
            return   AjaxResult.error("该老人已绑定，请勿重复绑定");
        }
        //5 如果没有绑定，插入数据
        FamilyMemberElder familyMemberElder = BeanUtil.toBean(memberElderDto, FamilyMemberElder.class);
        familyMemberElder.setElderId(elder.getId());
        familyMemberElder.setFamilyMemberId(Long.valueOf(userId));
        familyMemberElder.setCreateTime(new Date());
        familyMemberElderMapper.insert(familyMemberElder);;
        //6 返回
        return AjaxResult.success();
    }

    @GetMapping("/list-by-page")
    @Operation(summary = "分页查询接口",description = "分页查询家人接口，返回家人列表")
    public AjaxResult listByPage(@Parameter(description = "当前页码",required = true,example = "1") Integer pageNum,
                                 @Parameter(description = "每页条数",required = true,example = "10")Integer pageSize) {
        Long userId = BaseContext.getCurrentId();
        //2 查询
        List<MemberElderVo> memberElders = familyMemberElderMapper.listByPage(userId);
        //补点假数据，后期来真的数据
        for (MemberElderVo memberElder : memberElders) {
            memberElder.setTypeName("豪华单人间");
            memberElder.setIotId("fdgsfgdt563ergfd");
        }
        return AjaxResult.success(memberElders);
    }
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除接口",description = "删除家人接口，返回删除结果")
    public AjaxResult deleteById(@RequestParam Long id) {
        int result = familyMemberElderMapper.deleteById(id);
        //判断是否删除成功 result是影响行数
        return result>0?AjaxResult.success():AjaxResult.error();
    }

    @GetMapping("/my")
    @Operation(summary = "查询我的家人接口",description = "这个用来下单，探访预约时使用")
    public AjaxResult  my(){
        Long userId = BaseContext.getCurrentId();
        List<FamilyMemberElderVo> memberElders=familyMemberElderMapper.selectByMemberId(Long.valueOf(userId));

        return AjaxResult.success(memberElders);
    }


}
