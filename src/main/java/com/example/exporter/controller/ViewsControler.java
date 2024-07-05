package com.example.exporter.controller;

import com.example.exporter.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/views")
public class ViewsControler {

    private final ViewService viewService;

    @Autowired
    public ViewsControler(ViewService viewService) {
        this.viewService = viewService;
    }

    @PostMapping("/create")
    public void createView() {
        viewService.createViews();
    }
}
