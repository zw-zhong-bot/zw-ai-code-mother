package com.zw.zwaicodemother.common;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

@Slf4j
public class WebScreenshotUtils {
    private  static final WebDriver webDriver;
    static  {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT=900;
        webDriver=initChromeDriver(DEFAULT_WIDTH,DEFAULT_HEIGHT);
    }
    public  void  destroy(){
        webDriver.quit();
    }
    /**
     * 初始化 Chrome 浏览器驱动
     */
    @NotNull
    private static WebDriver initChromeDriver(int width, int height){
        try{
            //自动管理ChromeDriver
            WebDriverManager.chromedriver().setup();
            //配置chrome选项
            ChromeOptions chromeOptions=new ChromeOptions();
            //无头模式
            chromeOptions.addArguments("--headless");
            //禁用Gpu（在某些环境下避免问题）
            chromeOptions.addArguments("--disable-gpu");
            //禁用沙盒模式（Docker环境需要）
            chromeOptions.addArguments("--no-sandbox");
            //禁用开发者shm使用
            chromeOptions.addArguments("--disable-dev-shm-usage");
            //设置窗口大小
            chromeOptions.addArguments("--window-size%d,%d="+width+","+height);
            //禁用扩展
            chromeOptions.addArguments("--disable-extensions");
            //设置用户代理
            chromeOptions.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
           //创建驱动
            WebDriver driver=new ChromeDriver(chromeOptions);
            //设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            //设置隐形等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
           // //初始化驱动
            return driver;

        }catch (Exception e){
            log.error("初始化ChromeDriver失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"初始化ChromeDriver失败");
        }
    }
    /**
     * 保存图片到文件
     */
    private  static void saveImage(byte[] imageBytes,String  imagePath){
        try{
            FileUtil.writeBytes(imageBytes,imagePath);
            log.info("图片保存成功:{}",imagePath);
        }catch (Exception e){
            log.error("保存图片到文件失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"保存图片到文件失败");
        }
    }
    /**
     * 压缩图片
     */
    private  static  void compressImage(String  originalImagePath,String  compressedImagePath){
        //压缩图片质量(0.1=10%质量)
        final float COMPRESS_QUALITY=0.3f;
        try{
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESS_QUALITY
            );
        }catch (Exception e){
            log.error("压缩图片失败:{}-》{}",originalImagePath,compressedImagePath,e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"压缩图片失败");
        }
    }
    /**
     * 等待页面加载完成
     */
    private  static  void waitForPageLoad(WebDriver driver){
        try{
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            //// 等待 document.readyState 为complete
            wait.until(webDriver ->
                ((JavascriptExecutor)webDriver).executeAsyncScript("return document.readyState")
                        .equals("complete")
            );
            //额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        }catch (Exception e){
            log.error("等待页面加载时出现异常，继续执行截图",e);
            // 忽略异常
        }
    }
    /**
     * 生成网页截图
     *
     * @param webUrl 网页URL
     * @return 压缩后的截图文件路径，失败返回null
     */
    public static  String saveWebPageScreenshot(String webUrl){
        if (StrUtil.isBlank(webUrl)){
            log.error("网页URL为空");
            return null;
        }
        try {
            //创建临时目录
            String rootPath= System.getProperty("user.dir") + File.separator + "temp" + File.separator +
                    "screenshot" + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            //图片后缀
            final String IMAGE_SUFFIX =".png";
            //原始截图路径
            String imageSavePath=rootPath+File.separator+ RandomUtil.randomNumbers(5)
                    + IMAGE_SUFFIX;
            //访问网页
            webDriver.get(webUrl);
            //等待页面完成加载
            waitForPageLoad(webDriver);
            //截图
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            //保存原始截图
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始截图保存成功: {}", imageSavePath);
            //压缩截图
            final String COMPRESS_IMAGE_SUFFIX = "_compressed.JPG" ;
            String compressedImagePath = rootPath+File.separator+
                    RandomUtil.randomNumbers(5)+COMPRESS_IMAGE_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("压缩截图保存成功: {}", compressedImagePath);
            //删除原始截图
            FileUtil.del(imageSavePath);
            log.info("原始截图删除成功: {}", imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("网页截图失败: {}", webUrl, e);
            return null;
        }
    }


}
