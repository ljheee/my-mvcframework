package com.ljheee.mvc.demo.controller;

import com.ljheee.mvc.demo.service.IDemoService;
import com.ljheee.mvc.framework.annotation.MyAutowired;
import com.ljheee.mvc.framework.annotation.MyController;
import com.ljheee.mvc.framework.annotation.MyRequestMapping;
import com.ljheee.mvc.framework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by lijianhua04 on 2018/9/24.
 */
@MyController
@MyRequestMapping("/demo")
public class DemoController {

    @MyAutowired
    IDemoService demoService;

    @MyRequestMapping("/hello")
    public String hello(@MyRequestParam("name") String name) {
        System.out.println(demoService.say(name));
        return demoService.say(name);
    }


    /**
     * http://localhost:8080/mvc/demo/hi?name=ljh
     *
     * @param request
     * @param response
     * @param name
     */
    @MyRequestMapping("/hi")
    public void hi(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("name") String name) {
        try {
            response.getWriter().write("nihao! " + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
