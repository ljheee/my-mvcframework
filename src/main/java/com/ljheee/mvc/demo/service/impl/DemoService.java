package com.ljheee.mvc.demo.service.impl;

import com.ljheee.mvc.demo.service.IDemoService;
import com.ljheee.mvc.framework.annotation.MyService;

/**
 * Created by lijianhua04 on 2018/9/24.
 */
@MyService
public class DemoService implements IDemoService{


    @Override
    public String say(String name){
        return "hello,"+name;
    }

}