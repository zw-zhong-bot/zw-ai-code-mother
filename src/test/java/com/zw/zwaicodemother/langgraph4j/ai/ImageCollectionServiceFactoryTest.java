package com.zw.zwaicodemother.langgraph4j.ai;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

@SpringBootTest
class ImageCollectionServiceFactoryTest {
    @Resource
    private ImageCollectionService imageCollectionService;
    @Test
    void testTechWebsiteImageCollection() {
        String result = imageCollectionService.collectImages(
                "创建一个技术博客网站，需要展示编程教程和系统架构");
        Assertions.assertNotNull(result);
        System.out.println("技术网站收集到的图片: " + result);
    }
    void testEcommerceWebsiteImageCollection() {
        String result = imageCollectionService.collectImages("创建一个电商购物网站，需要展示商品和品牌形象");
        Assertions.assertNotNull(result);
        System.out.println("电商网站收集到的图片: " + result);
    }

}