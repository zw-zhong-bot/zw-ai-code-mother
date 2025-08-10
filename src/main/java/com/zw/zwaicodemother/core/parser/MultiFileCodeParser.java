package com.zw.zwaicodemother.core.parser;

import com.zw.zwaicodemother.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* 多文件代码解析器（HTML + CSS + JS）
* */
public class MultiFileCodeParser implements  CodeParser<MultiFileCodeResult> {
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult multiFileCodeResult = new MultiFileCodeResult();
        //提取各类代码
        String htmlCode =extractCodePattern(codeContent,HTML_CODE_PATTERN);
        String cssCode =extractCodePattern(codeContent,CSS_CODE_PATTERN);
        String jsCode =extractCodePattern(codeContent,JS_CODE_PATTERN);
        //设置Html代码
        if(htmlCode != null && !htmlCode.trim().isEmpty()){
            multiFileCodeResult.setHtmlCode(htmlCode.trim());
        }
        //设置Css代码
        if(cssCode != null && !cssCode.trim().isEmpty()){
            multiFileCodeResult.setCssCode(cssCode.trim());
        }
        //设置JS代码
        if(jsCode != null && !jsCode.trim().isEmpty()){
            multiFileCodeResult.setJsCode(jsCode.trim());
        }
        return multiFileCodeResult;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private String extractCodePattern(String content, Pattern pattern) {
        Matcher matcher =pattern.matcher(content);
        if(matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


}
