package com.demo.mememaker.controller;

import com.demo.mememaker.exception.UnsupportedFileFormat;
import com.demo.mememaker.graphic.Meme;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Objects;

/**
 * Controller class that handles images upload.
 *
 * @author Matthew Mazzotta
 */
@Controller
public class MemeController {

    private static final String CONTENT_DISPOSITION_HEADER_VALUE = "attachment; filename=meme.%s";
    private static final String SPLIT_AT_DOT_REGEX = "\\.";

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/")
    public ResponseEntity<StreamingResponseBody> handleFileUpload(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "") String topText,
            @RequestParam(defaultValue = "") String bottomText) {
        Meme.Format format = getFormat(file);
        StreamingResponseBody responseBody = outputStream -> Meme.draw(file, outputStream, format, topText, bottomText);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_HEADER_VALUE, format.getFormat()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
    }

    private Meme.Format getFormat(MultipartFile file) {
        try {
            String format = Objects.requireNonNull(file.getOriginalFilename()).split(SPLIT_AT_DOT_REGEX)[1].toUpperCase();
            return Meme.Format.valueOf(format);
        } catch(Exception e) {
            throw new UnsupportedFileFormat();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }
}