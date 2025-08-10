package com.zw.zwaicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.ai.model.MultiFileCodeResult;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return  CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        //保存HTML文件
        writteToFile(baseDirPath,"index.html",result.getHtmlCode());
        //保存CSS文件
        writteToFile(baseDirPath,"style.css",result.getCssCode());
        //保存JS文件
        writteToFile(baseDirPath,"script.js",result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        //至少要有Html代码，Css和Js可以为空
        if (StrUtil.isBlank(result.getHtmlCode())){
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"HTML代码不能为空");
        }
    }
}
