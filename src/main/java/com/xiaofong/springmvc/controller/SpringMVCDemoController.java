package com.xiaofong.springmvc.controller;

import com.xiaofong.springmvc.annotation.MyController;
import com.xiaofong.springmvc.annotation.MyControllerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyControllerMapping("/test")
public class SpringMVCDemoController {

    @MyControllerMapping("test01")
    public void test01(HttpServletRequest request, HttpServletResponse response, String param) {

        System.out.println(param);
        try {
            response.getWriter().write("test01 method call success! param:" + param);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
