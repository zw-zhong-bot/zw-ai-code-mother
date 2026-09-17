package com.zw.zwaicodemother.ai.provider.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zw.zwaicodemother.ai.provider.ImageItem;
import com.zw.zwaicodemother.ai.provider.ImageSearchProvider;
import com.zw.zwaicodemother.ai.provider.ModelOptions;
import com.zw.zwaicodemother.model.enums.ModelProviderEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 图片搜索提供方。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 * @since 2026-09-17
 */
@Slf4j
@Component
public class PexelsImageSearchProvider implements ImageSearchProvider {

    /**
     * 默认接口地址
     */
    private static final String DEFAULT_BASE_URL = "https://api.pexels.com/v1/search";

    @Override
    public ModelProviderEnum provider() {
        return ModelProviderEnum.PEXELS;
    }

    @Override
    public List<ImageItem> search(ModelOptions options, String query, int count) throws Exception {
        if (StrUtil.isBlank(options.getApiKey())) {
            throw new IllegalStateException("模型配置缺少 apiKey：" + options.getConfigName());
        }
        String url = StrUtil.blankToDefault(options.getBaseUrl(), DEFAULT_BASE_URL);
        List<ImageItem> imageList = new ArrayList<>();
        try (HttpResponse response = HttpRequest.get(url)
                .header("Authorization", options.getApiKey())
                .form("query", query)
                .form("per_page", count)
                .form("page", 1)
                .execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("Pexels 接口返回异常，状态码：" + response.getStatus());
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray photos = result.getJSONArray("photos");
            if (photos == null) {
                return imageList;
            }
            for (int i = 0; i < photos.size(); i++) {
                JSONObject photo = photos.getJSONObject(i);
                JSONObject src = photo.getJSONObject("src");
                if (src == null || StrUtil.isBlank(src.getStr("medium"))) {
                    continue;
                }
                imageList.add(new ImageItem(src.getStr("medium"), photo.getStr("alt", query)));
            }
        }
        return imageList;
    }
}
