package com.zw.zwaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zw.zwaicodemother.ai.AiCodeGenTypeRoutingService;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.constant.AppConstant;
import com.zw.zwaicodemother.core.AiCodeGeneratorFacade;
import com.zw.zwaicodemother.core.builder.VueProjectBuilder;
import com.zw.zwaicodemother.core.handler.StreamHandlerExecutor;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.mapper.AppMapper;
import com.zw.zwaicodemother.model.dto.app.AppAddRequest;
import com.zw.zwaicodemother.model.dto.app.AppQueryRequest;
import com.zw.zwaicodemother.model.entity.App;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zw.zwaicodemother.model.vo.AppVO;
import com.zw.zwaicodemother.model.vo.UserVO;
import com.zw.zwaicodemother.service.AppService;
import com.zw.zwaicodemother.service.ChatHistoryService;
import com.zw.zwaicodemother.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.View;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
@Log4j2
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private final UserServiceImpl userServiceImpl;
    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    public AppServiceImpl(UserServiceImpl userServiceImpl, AiCodeGeneratorFacade aiCodeGeneratorFacade) {
        this.userServiceImpl = userServiceImpl;
        this.aiCodeGeneratorFacade = aiCodeGeneratorFacade;
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
//        关联查询用户信息
        Long userId = app.getUserId();
        if(userId != null) {
            User user=userServiceImpl.getById(userId);
            UserVO  userVO =  userServiceImpl.getUserVo(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
//        批量获取用户信息，避免N+1查询问题
        Set<Long> userId  =appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long,UserVO> userVOMap =userServiceImpl.listByIds(userId).stream()
                .collect(Collectors.toMap(User::getId,userServiceImpl::getUserVo));
        return appList.stream().map(app -> {
            AppVO appVO=getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public Page<AppVO> getFeaturedAppPage(AppQueryRequest appQueryRequest) {
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        // 限制每页最多20个
        pageSize = Math.min(pageSize, 20);
        
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest);
        // 精选应用按优先级排序，优先级高的在前
        queryWrapper.orderBy("priority", false);
        
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return appVOPage;
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        //1.参数校验
        ThrowUtils.throwIf(appId == null  || appId<=0,ErrorCode.PARAMS_ERROR,"应用id不能为空" );
        ThrowUtils.throwIf(loginUser ==  null,ErrorCode.NOT_LOGIN_ERROR,"用户未登录");
//        2.查询应用信息
        App app=this.getById(appId);
        ThrowUtils.throwIf(app ==  null,ErrorCode.NOT_FOUND_ERROR,"应用不存在");
//        3.验证用户是否有权限部署该应用，仅本人可以部署
        if(!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限部署该应用");
        }
//        4.检查是否已有deployKey
        String deployKey = app.getDeployKey();
//        没有则生成6位deployKey（大小写字母+数字）
        if (StrUtil.isBlank(deployKey)){
            deployKey = RandomUtil.randomString(6);
        }
//        5.获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName=codeGenType+"_"+appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR+ File.separator+sourceDirName;
//        6.检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if(!sourceDir.exists()||!sourceDir.isDirectory()){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"应用代码不存在，请先生成代码");

        }
//        7.Vue项目特殊处理：执行构建；
        CodeGenTypeEnum codeGenTypeEnum=CodeGenTypeEnum.getCodeGenTypeEnum(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            //Vue项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
//        8.复制文件到部署目录
        String deployDirPath =AppConstant.CODE_DEPLOY_ROOT_DIR+File.separator+deployKey;
        try {
            FileUtil.copyContent(sourceDir,new File(deployDirPath),true);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"部署失败："+e.getMessage());
        }
//        9.更新应用的deployKey和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult,ErrorCode.OPERATION_ERROR,"更新应用部署信息失败");
//        10.构建应用访问 URL
        String appDeployUrl=String.format("%s/%s/",AppConstant.CODE_DEPLOY_HOST,deployKey);
        //11.异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
//        1.参数校验
        ThrowUtils.throwIf(appId == null ||appId<=0 ,ErrorCode.PARAMS_ERROR,"应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message),ErrorCode.PARAMS_ERROR,"用户消息不能为空");
//        2.查询用户信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null,ErrorCode.PARAMS_ERROR,"应用不存在");
//        3.验证用户是否有权限访问该应用，仅本人可以生成代码
        if(!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"无权限访问该应用");

        }
//        4.获取应用生成的代码类型
        String  codeGenTypeStr =app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getCodeGenTypeEnum(codeGenTypeStr);
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型");

        }
//        5.通过校验后，同时添加用户消息到对话历史
        chatHistoryService.addChatMessage(
                appId,message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        //   6.调用AI生成代码
        Flux<String> contentFlux =aiCodeGeneratorFacade.generateAndSaveCodeStream(message,codeGenTypeEnum,appId);
        //   7.收集AI响应内容并在完成记录后记录到对话历史
        return streamHandlerExecutor.doExecute(contentFlux,chatHistoryService,
                appId,loginUser,codeGenTypeEnum);
        /*StringBuilder  aiResponseBuilder  = new StringBuilder();
        return contentFlux.map(chunk ->{
            //收集AI响应内容
            aiResponseBuilder.append(chunk);
            return chunk;
        }).doOnComplete(() ->{
            // 流式响应完成后，添加AI消息到对话历史
            String aiResponse = aiResponseBuilder.toString();
            if(StrUtil.isNotBlank(aiResponse)){
                chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
            }
        }).doOnError(error->{
            //如果AI回复失败，也记录到错误消息
            String errorMessage = "AI回复失败：" + error.getMessage();
            chatHistoryService.addChatMessage(appId,errorMessage,ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
        });*/
    }

    @Override
    public String uploadAppCover(Long appId, MultipartFile file, User loginUser) {
        // 1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "封面文件不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        
        // 2.查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        // 3.验证用户是否有权限上传封面，仅本人可以上传
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限上传该应用的封面");
        }
        
        try {
            // 4.创建封面存储目录
            String coverDirPath = System.getProperty("user.dir") + "/src/main/resources/static/fengmian";
            File coverDir = new File(coverDirPath);
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }
            
            // 5.生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = FileUtil.getSuffix(originalFilename);
            String fileName = "app_cover_" + appId + "_" + RandomUtil.randomString(8) + "." + fileExtension;
            
            // 6.保存文件
            String filePath = coverDirPath + File.separator + fileName;
            file.transferTo(new File(filePath));
            
            // 7.生成访问URL
            String coverUrl = "/api/static/fengmian/" + fileName;
            
            // 8.更新应用封面信息
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(coverUrl);
            updateApp.setUpdateTime(LocalDateTime.now());
            boolean updateResult = this.updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用封面信息失败");
            
            return coverUrl;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "封面上传失败：" + e.getMessage());
        }
    }
    
    @Override
    public String getAppCover(Long appId, User loginUser) {
        // 1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        
        // 2.查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        
        // 3.获取封面URL，不使用默认封面
        return app.getCover();
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        //转换为Long类型
        Long appId =Long.valueOf (id.toString());
        if (appId <= 0) {
            return false;
        }
        //先删除关联的对话历史
        try{
            chatHistoryService.deleteByAppId(appId);
        }catch (Exception e){
            // 记录日志但不阻止应用删除
            log.error("删除应用时关联删除对话历史失败，应用ID: {}", appId, e);
        }
        return super.removeById(id);
    }

    /**
     * 创建应用
     *
     * @param appAddRequest 应用添加请求
     * @param loginUser 登录用户
     * @return 应用ID
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 1.参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt),ErrorCode.PARAMS_ERROR,"初始化 prompt 不能为空");

        // 2.创建应用
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        //使用AI智能选择代码生成类型
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        //插入数据库
        boolean saveResult = this.save(app);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "创建应用失败");
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() ->{
            //// 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            updateApp.setUpdateTime(LocalDateTime.now());
            boolean updateResult = this.updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }
}