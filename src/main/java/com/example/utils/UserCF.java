package com.example.utils;

import cn.hutool.core.collection.CollectionUtil;
import com.example.entity.RelateDTO;

import java.util.*;
import java.util.stream.Collectors;

public class UserCF {

    /**
     * 方法描述: 推荐商品id列表
     *
     * @param userId 当前用户
     * @param list   用户商品评分数据
     * @return {@link List<Integer>}
     */
    public static List<Integer> recommend(Integer userId, List<RelateDTO> list) {
        // 按用户分组
        Map<Integer, List<RelateDTO>> userMap = list.stream().collect(Collectors.groupingBy(RelateDTO::getUseId));
        // 获取其他用户与当前用户的关系值
        Map<Integer, Double> userDisMap = CoreMath.computeNeighbor(userId, userMap, 0);
        if (CollectionUtil.isEmpty(userDisMap)) {
            return Collections.emptyList();
        }
        // 按相似度降序排列，取Top-N个相似用户
        List<Map.Entry<Integer, Double>> sortedNeighbors = userDisMap.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        if (sortedNeighbors.isEmpty()) {
            return Collections.emptyList();
        }
        // 当前用户看过的商品列表
        List<Integer> userItems = userMap.get(userId).stream().map(RelateDTO::getGoodsId).collect(Collectors.toList());
        // 收集多个相似用户的推荐商品，按相似度加权
        Map<Integer, Double> goodsScoreMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> neighbor : sortedNeighbors) {
            Integer neighborId = neighbor.getKey();
            Double similarity = neighbor.getValue();
            List<Integer> neighborItems = userMap.get(neighborId).stream()
                    .map(RelateDTO::getGoodsId)
                    .collect(Collectors.toList());
            // 找到该相似用户看过但当前用户没看过的商品
            for (Integer goodsId : neighborItems) {
                if (!userItems.contains(goodsId)) {
                    goodsScoreMap.merge(goodsId, similarity, Double::sum);
                }
            }
        }
        // 按加权得分降序返回商品id
        return goodsScoreMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}
