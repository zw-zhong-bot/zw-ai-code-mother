package com.zw.zwaicodemother.service;

import com.zw.zwaicodemother.model.entity.App;
import com.zw.zwaicodemother.model.vo.AppVO;
import com.zw.zwaicodemother.service.AppService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppService 测试类
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
@SpringBootTest
public class AppServiceTest {

    @Resource
    private AppService appService;

    @Test
    public void testGetAppVO() {
        // 测试获取应用视图
        App app = new App();
        app.setId(1L);
        app.setAppName("测试应用");
        app.setInitPrompt("测试prompt");
        
        AppVO appVO = appService.getAppVO(app);
        assertNotNull(appVO);
        assertEquals(app.getId(), appVO.getId());
        assertEquals(app.getAppName(), appVO.getAppName());
        assertEquals(app.getInitPrompt(), appVO.getInitPrompt());
    }
}
