package com.demo.mememaker.exception;

/**
 * Exception class thrown when the application attempts to meme a file with a
 * format that's not supported.
 *
 * {@see com.demo.mememaker.graphic.Meme.Format}
 *
 * @author Matthew Mazzotta
 */
public class UnsupportedFileFormat extends RuntimeException { }