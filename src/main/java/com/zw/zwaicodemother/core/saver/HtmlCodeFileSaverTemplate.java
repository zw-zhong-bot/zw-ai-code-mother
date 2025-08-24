package com.zw.zwaicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.zw.zwaicodemother.ai.enums.CodeGenTypeEnum;
import com.zw.zwaicodemother.ai.model.HtmlCodeResult;
import com.zw.zwaicodemother.exception.BusinessException;
import com.zw.zwaicodemother.exception.ErrorCode;

/**
 * HTML代码文件保存器
 *
 *
 */

public class HtmlCodeFileSaverTemplate extends  CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
       //HTML代码不能为空
        if(StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"HTML代码不能为空");
        }
    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        //保存HTML文件
        writteToFile(baseDirPath,"index.html",result.getHtmlCode());

    }

}
