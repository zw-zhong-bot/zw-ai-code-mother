package com.zw.zwaicodemother.common;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
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
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
public class WebScreenshotUtils {
    private  static final WebDriver webDriver;
    static  {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT=900;
        webDriver=initChromeDriver();
    }

    public  void  destroy(){
        webDriver.quit();
    }
    /**
     * 初始化 Chrome 浏览器驱动
     */
    @NotNull
    private static WebDriver initChromeDriver(){
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
            chromeOptions.addArguments("--window-size%d,%d="+ 1600 +","+ 900);
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
    private  static  void waitForPageLoad(){
        try{
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(WebScreenshotUtils.webDriver, Duration.ofSeconds(30));
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
            waitForPageLoad();
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

    /**
     * 清理过期的临时文件
     * 
     * @param expireHours 过期时间阈值（小时）
     * @return 清理的文件数量
     */
    public static int cleanupTempFiles(int expireHours) {
        if (expireHours <= 0) {
            log.warn("过期时间阈值必须大于0，当前值: {}", expireHours);
            return 0;
        }
        
        int deletedCount = 0;
        try {
            // 获取临时目录路径
            String tempPath = System.getProperty("user.dir") + File.separator + "temp" + File.separator + "screenshot";
            File tempDir = new File(tempPath);
            
            if (!tempDir.exists() || !tempDir.isDirectory()) {
                log.info("临时目录不存在或不是目录: {}", tempPath);
                return 0;
            }
            
            // 计算过期时间点
            long expireTime = System.currentTimeMillis() - (expireHours * 60 * 60 * 1000L);
            LocalDateTime expireDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(expireTime), 
                ZoneId.systemDefault()
            );
            
            log.info("开始清理过期临时文件，过期时间阈值: {}小时，过期时间点: {}", expireHours, expireDateTime);
            
            // 遍历临时目录下的所有子目录
            File[] subDirs = tempDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    try {
                        // 检查子目录的最后修改时间
                        long lastModified = subDir.lastModified();
                        if (lastModified < expireTime) {
                            // 删除过期的子目录及其所有内容
                            boolean deleted = FileUtil.del(subDir);
                            if (deleted) {
                                deletedCount++;
                                log.info("删除过期临时目录: {}, 最后修改时间: {}", 
                                    subDir.getAbsolutePath(), 
                                    LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(lastModified), 
                                        ZoneId.systemDefault()
                                    )
                                );
                            } else {
                                log.warn("删除过期临时目录失败: {}", subDir.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理临时目录时发生异常: {}", subDir.getAbsolutePath(), e);
                    }
                }
            }
            
            // 清理临时目录下的孤立文件
            File[] files = tempDir.listFiles(File::isFile);
            if (files != null) {
                for (File file : files) {
                    try {
                        long lastModified = file.lastModified();
                        if (lastModified < expireTime) {
                            boolean deleted = FileUtil.del(file);
                            if (deleted) {
                                deletedCount++;
                                log.info("删除过期临时文件: {}, 最后修改时间: {}", 
                                    file.getAbsolutePath(), 
                                    LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(lastModified), 
                                        ZoneId.systemDefault()
                                    )
                                );
                            } else {
                                log.warn("删除过期临时文件失败: {}", file.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理临时文件时发生异常: {}", file.getAbsolutePath(), e);
                    }
                }
            }
            
            log.info("临时文件清理完成，共删除 {} 个过期文件/目录", deletedCount);
            
        } catch (Exception e) {
            log.error("清理临时文件时发生异常", e);
        }
        
        return deletedCount;
    }

    /**
     * 清理过期的临时文件（默认24小时过期）
     * 
     * @return 清理的文件数量
     */
    public static int cleanupTempFiles() {
        // 默认24小时过期
        final int DEFAULT_EXPIRE_HOURS = 24;
        return cleanupTempFiles(DEFAULT_EXPIRE_HOURS);
    }


}
