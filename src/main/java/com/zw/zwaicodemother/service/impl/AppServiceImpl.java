package com.zw.zwaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.constant.AppConstant;
import com.zw.zwaicodemother.core.AiCodeGeneratorFacade;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;
import com.zw.zwaicodemother.exception.ThrowUtils;
import com.zw.zwaicodemother.mapper.AppMapper;
import com.zw.zwaicodemother.model.dto.app.AppQueryRequest;
import com.zw.zwaicodemother.model.entity.App;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.vo.AppVO;
import com.zw.zwaicodemother.model.vo.UserVO;
import com.zw.zwaicodemother.service.AppService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
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
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    private final UserServiceImpl userServiceImpl;
    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;

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
//        7.复制文件到部署目录
        String deployDirPath =AppConstant.CODE_DEPLOY_ROOT_DIR+File.separator+deployKey;
        try {
            FileUtil.copyContent(sourceDir,new File(deployDirPath),true);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"部署失败："+e.getMessage());
        }
//        8.更新应用的deployKey和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult,ErrorCode.OPERATION_ERROR,"更新应用部署信息失败");
//        9.返回可访问的URL
        return String.format("%s/%s/",AppConstant.CODE_DEPLOY_HOST,deployKey);
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
//        5.调用AI生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message,codeGenTypeEnum,appId);
    }
}
