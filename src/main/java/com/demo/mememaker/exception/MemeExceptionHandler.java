package com.demo.mememaker.exception;

import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.imageio.IIOException;

/**
 * Handles common exception thrown in the application redirecting the user to the landing page, adding
 * an error message.
 *
 * @author Matthew Mazzotta
 */
@ControllerAdvice
public class MemeExceptionHandler {

    private static final String REDIRECT_TO_INDEX_PAGE = "/";
    private static final String EXCEPTION_MODEL_ATTRIBUTE_NAME = "exception";
    private static final String EXCEPTION_MODEL_ATTRIBUTE_GIF_VALUE = "Gif doesn't contain metadata, might be corrupted";
    private static final String EXCEPTION_MODEL_ATTRIBUTE_FORMAT_VALUE = "Must be a gif, png, jpg or jpeg file";
    private static final String EXCEPTION_MODEL_ATTRIBUTE_SIZE_VALUE = "File size exceeded";
    private static final String EXCEPTION_MODE_ATTRIBUTE_GENERIC_VALUE = "Something went wrong";

    @ExceptionHandler(IIOException.class)
    public RedirectView badGifHandler(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(EXCEPTION_MODEL_ATTRIBUTE_NAME, EXCEPTION_MODEL_ATTRIBUTE_GIF_VALUE);
        return new RedirectView(REDIRECT_TO_INDEX_PAGE);
    }

    @ExceptionHandler(UnsupportedFileFormat.class)
    public RedirectView badFileFormatHandler(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(EXCEPTION_MODEL_ATTRIBUTE_NAME, EXCEPTION_MODEL_ATTRIBUTE_FORMAT_VALUE);
        return new RedirectView(REDIRECT_TO_INDEX_PAGE);
    }

    @ExceptionHandler(SizeLimitExceededException.class)
    public RedirectView sizeLimitExceededHandler(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(EXCEPTION_MODEL_ATTRIBUTE_NAME, EXCEPTION_MODEL_ATTRIBUTE_SIZE_VALUE);
        return new RedirectView(REDIRECT_TO_INDEX_PAGE);
    }

    @ExceptionHandler(Exception.class)
    public RedirectView lastResortHandler(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(EXCEPTION_MODEL_ATTRIBUTE_NAME, EXCEPTION_MODE_ATTRIBUTE_GENERIC_VALUE);
        return new RedirectView(REDIRECT_TO_INDEX_PAGE);
    }
}