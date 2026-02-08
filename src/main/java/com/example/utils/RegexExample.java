package com.example.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 文件名称匹配类
 */
@Data
@Slf4j
public class RegexExample {
    private String pattern = ".*统计表\\.xlsx";
    private List<String> patterns;


    /**
     * 校验文件名称是否匹配
     * @param fileName
     * @return
     */
    public boolean matches(String fileName){
        log.info("matches fileName:{}", fileName);
        return null == fileName ? false : fileName.trim().matches(pattern);
    }

    /**
     * 校验文件名称是否匹配
     * @param fileName  文件名称
     * @param count     是否匹配，匹配名单个数。0名单内全部匹配，
     * @return
     */
    public boolean matches(String fileName, int count){
        log.info("matches fileName:{} | count = {}", fileName, count);
        if(null == fileName)
            return false;
        if(0 == count){
            int i = 0;
            for (String pattern : patterns) {
                if(!fileName.trim().matches(pattern)){//判断不匹配
                    i++;
                }
            }
            if(i == patterns.size())
                return false;
            else
                return true;
        }else {
            if(count <= patterns.size()){
                int a = 0;
                for (int i = 0; i < count; i++) {
                    if(!fileName.trim().matches(patterns.get(i))){//判断不匹配
                        a++;
                    }
                }
                if(a == count)
                    return false;
                else
                    return true;
            } else {
                int i = 0;
                for (String pattern : patterns) {
                    if(!fileName.trim().matches(pattern)){//判断不匹配
                        i++;
                    }
                }
                if(i == patterns.size())
                    return false;
                else
                    return true;
            }
        }
    }
}
