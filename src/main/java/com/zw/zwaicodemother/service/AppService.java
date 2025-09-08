package com.zw.zwaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zw.zwaicodemother.model.dto.app.AppQueryRequest;
import com.zw.zwaicodemother.model.entity.App;
import com.zw.zwaicodemother.model.entity.User;
import com.zw.zwaicodemother.model.vo.AppVO;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.List;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/zw-zhong-bot">程序员zw</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取应用视图
     *
     * @param app 应用
     * @return 应用视图
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图列表
     *
     * @param appList 应用列表
     * @return 应用视图列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 应用查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 应用查询请求
     * @return 精选应用分页
     */
    Page<AppVO> getFeaturedAppPage(AppQueryRequest appQueryRequest);


    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 应用部署
     *
     * @param appId 应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    public String deployApp(Long appId, User loginUser);

    /**
     * 上传应用封面
     *
     * @param appId 应用ID
     * @param file 封面文件
     * @param loginUser 登录用户
     * @return 封面图片URL
     */
    String uploadAppCover(Long appId, MultipartFile file, User loginUser);
    
    /**
     * 获取应用封面
     *
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 封面图片URL
     */
    String getAppCover(Long appId, User loginUser);

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean removeById(Serializable id);

}
