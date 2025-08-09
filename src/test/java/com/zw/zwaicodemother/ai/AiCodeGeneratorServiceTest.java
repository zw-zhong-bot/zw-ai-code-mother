package com.zw.zwaicodemother.ai;

import com.zw.zwaicodemother.ai.model.HtmlCodeResult;
import com.zw.zwaicodemother.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AiCodeGeneratorServiceTest {
    @Resource
    private  AiCodeGeneratorService  aiCodeGeneratorService;
    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员zw的工作记录小工具");
        Assertions.assertNotNull(result);
    }
    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个程序员zw的留言板");
        Assertions.assertNotNull(multiFileCode);
    }

}
