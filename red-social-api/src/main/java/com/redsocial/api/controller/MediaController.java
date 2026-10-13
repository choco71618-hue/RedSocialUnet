package com.redsocial.api.controller;

import com.redsocial.api.service.MediaService;
import com.redsocial.common.dto.MediaDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public MediaDto upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "folder", defaultValue = "redsocial") String folder) {
        MediaService.UrlInfo info = mediaService.upload(file, folder);
        return new MediaDto(info.url(), info.publicId());
    }
}