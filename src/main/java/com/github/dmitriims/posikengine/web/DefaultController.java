package com.github.dmitriims.posikengine.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {
    private final String homepage = "${search-engine-properties.home-page}";

    @RequestMapping(value =homepage)
    public String index() {
        return "index";
    }

    @RequestMapping(value ="/")
    public String notAdmin() {
        return "greeting";
    }

    @RequestMapping(value ="/login")
    public String login() {
        return "login";
    }

    @RequestMapping(value = "/denied")
    public String denied() {
        return "denied";
    }
}
