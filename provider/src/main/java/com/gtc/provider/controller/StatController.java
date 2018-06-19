package com.gtc.provider.controller;

import com.gtc.provider.controller.dto.stat.StatDto;
import com.gtc.provider.service.ClientStatProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.gtc.provider.config.Const.Rest.CLIENT;
import static com.gtc.provider.config.Const.Rest.STAT;

/**
 * Created by Valentyn Berezin on 01.01.18.
 */
@RestController
@RequestMapping(path = STAT)
public class StatController {

    private final ClientStatProvider statProvider;

    public StatController(ClientStatProvider statProvider) {
        this.statProvider = statProvider;
    }

    @GetMapping(CLIENT)
    public StatDto getStats() {
        return statProvider.calculateStats();
    }
}
