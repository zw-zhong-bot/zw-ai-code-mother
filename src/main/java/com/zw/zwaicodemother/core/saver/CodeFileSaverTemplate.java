package com.zw.zwaicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.constant.AppConstant;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;

import java.io.File;
import java.nio.charset.StandardCharsets;

/*
* 抽象代码文件保存器 - 模板方法模式
* */
public abstract class CodeFileSaverTemplate<T> {
    //文件保存根目录
    protected static final String FLILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法：保存代码的标准流程
     *
     * @param result 代码结果对象
     * @return 保存的目录
     */
    public final File saveCode(T result,Long appId){
        //1.验证输入
        validateInput(result);
        //2.构建唯一目录
        String baseDirPath = buildUniqueDir(appId);
        //3.保存文件（具体实现由子类提供）
        saveFiles(result,baseDirPath);
        //4.返回目录文件对象
        return  new File(baseDirPath);
    }
    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void  validateInput(T result){
        if(result == null){
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"代码结果对象不能为空");
        }
    }
    /**
     * 构建唯一目录路径
     *
     * @return 目录路径
     */
    protected final  String buildUniqueDir(Long appId){
        if(appId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"应用id不能为空");
        }
        String codeType= getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}",codeType, appId);
        String dirPatth = FLILE_SAVE_ROOT_DIR  + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPatth);
        return dirPatth;
    }


    /**
     * 写入单个文件的工具方法
     *
     * @param dirPath  目录路径
     * @param filename 文件名
     * @param content  文件内容
     */
    protected final void writteToFile(String dirPath ,String filename ,String content){
        if (StrUtil.isNotBlank(filename)){
            String filePath =dirPath + File.separator + filename;
            FileUtil.writeString(content,filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件的具体实现（由子类实现）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
