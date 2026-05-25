package cn.yangeit.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WechatService {

    private static final String REQUEST_URL =
            "https://api.weixin.qq.com/sns/jscode2session?grant_type=authorization_code";
    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
    private static final String PHONE_REQUEST_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=";

    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    public String getOpenid(String code) {
        Map<String, Object> params = getAppConfig();
        params.put("js_code", code);

        String result = HttpUtil.get(REQUEST_URL, params);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (ObjectUtil.isNotEmpty(jsonObject.getInt("errcode"))) {
            throw new RuntimeException(jsonObject.getStr("errmsg"));
        }
        return jsonObject.getStr("openid");
    }

    private Map<String, Object> getAppConfig() {
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        return params;
    }

    public String getPhone(String phoneCode) {
        String token = getToken();
        String url = PHONE_REQUEST_URL + token;

        Map<String, Object> params = new HashMap<>();
        params.put("code", phoneCode);

        String result = HttpUtil.post(url, JSONUtil.toJsonStr(params));
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (jsonObject.getInt("errcode") != 0) {
            throw new RuntimeException(jsonObject.getStr("errmsg"));
        }
        return jsonObject.getJSONObject("phone_info").getStr("phoneNumber");
    }

    private String getToken() {
        Map<String, Object> appConfig = getAppConfig();
        String result = HttpUtil.get(TOKEN_URL, appConfig);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (ObjectUtil.isNotEmpty(jsonObject.getInt("errcode"))) {
            throw new RuntimeException(jsonObject.getStr("errmsg"));
        }
        return jsonObject.getStr("access_token");
    }
}
