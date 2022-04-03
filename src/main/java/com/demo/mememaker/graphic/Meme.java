package com.demo.mememaker.graphic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class contains the methods to meme an image.
 *
 * Supported files are gif, jpg, png and jpeg.
 *
 * @author Matthew Mazzotta
 */
public class Meme {

    private static final Font DEFAULT_FONT = new Font("Arial", Font.BOLD, 40);

    enum Position { TOP, BOTTOM }

    @AllArgsConstructor
    @Getter
    public enum Format {
        GIF("gif"),
        JPG("jpg"),
        PNG("png"),
        JPEG("jpeg");

        private final String format;
    }

    private record ImageFrame(BufferedImage image, int delay, String disposal) {}

    private Meme() {}

    /**
     * Draws the top text and the bottom text in input on the uploaded file.
     *
     * @param file the file in input to draw on
     * @param outputStream the stream that will contain the drawn image
     * @param format the format of the image
     * @param topText the text to draw on the top of the image
     * @param bottomText the text to draw on the bottom of the image
     * @throws IOException if there is any low level I/O error
     */
    public static void draw(MultipartFile file, OutputStream outputStream, Format format, String topText, String bottomText) throws IOException {
        switch(format) {
            case JPG, PNG, JPEG -> memeImage(file, outputStream, format, topText, bottomText);
            case GIF -> memeGif(file, outputStream, topText, bottomText);
        }
    }

    private static void memeImage(MultipartFile file, OutputStream outputStream, Format format, String topText, String bottomText) throws IOException {
        try(InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            addText(image, topText, bottomText);
            ImageIO.write(image, format.getFormat(), outputStream);
        }
    }

    private static void addText(BufferedImage image, String topText, String bottomText) {
        if(!topText.isBlank())
            drawText(topText, image, Position.TOP);
        if(!bottomText.isBlank())
            drawText(bottomText, image, Position.BOTTOM);
    }

    private static void drawText(String text, BufferedImage image, Position position) {
        Graphics graphics = image.getGraphics();
        Font sizedFont = getFont(text, image, graphics);
        AttributedString attributedText = getAttributedString(text, sizedFont);

        FontMetrics metrics = graphics.getFontMetrics(sizedFont);
        int positionX = (image.getWidth() - metrics.stringWidth(text)) / 2;
        int positionY = switch(position) {
            case TOP -> metrics.getAscent();
            case BOTTOM -> (image.getHeight() - metrics.getHeight()) + metrics.getAscent();
        };
        graphics.drawString(attributedText.getIterator(), positionX, positionY);
    }

    private static AttributedString getAttributedString(String text, Font sizedFont) {
        AttributedString attributedText = new AttributedString(text);
        attributedText.addAttribute(TextAttribute.FONT, sizedFont);
        attributedText.addAttribute(TextAttribute.FOREGROUND, Color.WHITE);
        return attributedText;
    }

    private static Font getFont(String text, BufferedImage image, Graphics graphics) {
        Font sizedFont = DEFAULT_FONT;
        FontMetrics ruler = graphics.getFontMetrics(sizedFont);
        GlyphVector vector = sizedFont.createGlyphVector(ruler.getFontRenderContext(), text);

        Shape outline = vector.getOutline(0, 0);

        double expectedWidth = outline.getBounds().getWidth();
        double expectedHeight = outline.getBounds().getHeight();
        boolean textFits = image.getWidth() >= expectedWidth && image.getHeight() >= expectedHeight;

        if(!textFits) {
            double widthBasedFontSize = (sizedFont.getSize2D() * image.getWidth()) / expectedWidth;
            double heightBasedFontSize = (sizedFont.getSize2D() * image.getHeight()) / expectedHeight;

            double newFontSize = Math.min(widthBasedFontSize, heightBasedFontSize);
            sizedFont = sizedFont.deriveFont(sizedFont.getStyle(), (float) newFontSize);
        }
        return sizedFont;
    }

    private static void memeGif(MultipartFile file, OutputStream outputStream, String topText, String bottomText) throws IOException {
        List<ImageFrame> frames = getFrames(file.getInputStream());
        int fixedDelay = fixDelay(frames.get(0).delay);
        frames.forEach(frame -> addText(frame.image(), topText, bottomText));

        try(ImageOutputStream output = ImageIO.createImageOutputStream(outputStream);
            GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, fixedDelay, true)) {

            for(ImageFrame imageFrame : frames)
                writer.writeToSequence(imageFrame.image());
        }
    }

    private static List<ImageFrame> getFrames(InputStream gif) throws IOException {
        List<ImageFrame> frames = new ArrayList<>();
        int width = -1;
        int height = -1;

        ImageReader imageReader = ImageIO.getImageReadersByFormatName("gif").next();
        imageReader.setInput(ImageIO.createImageInputStream(gif));

        IIOMetadata metadata = imageReader.getStreamMetadata();
        if(metadata != null) {
            IIOMetadataNode globalRoot = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

            NodeList globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");

            if(globalScreenDescriptor.getLength() > 0) {
                IIOMetadataNode screenDescriptor = (IIOMetadataNode) globalScreenDescriptor.item(0);

                if(screenDescriptor != null) {
                    width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
                    height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
                }
            }
        }

        BufferedImage master = null;
        Graphics2D masterGraphics = null;

        for (int frameIndex = 0;; frameIndex++) {
            BufferedImage image;
            try {
                image = imageReader.read(frameIndex);
            } catch (IndexOutOfBoundsException io) {
                break;
            }

            if (width == -1 || height == -1) {
                width = image.getWidth();
                height = image.getHeight();
            }

            IIOMetadataNode root = (IIOMetadataNode) imageReader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0");
            IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
            int delay = Integer.parseInt(gce.getAttribute("delayTime"));
            String disposal = gce.getAttribute("disposalMethod");

            int x = 0;
            int y = 0;

            if (master == null) {
                master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                masterGraphics = master.createGraphics();
                masterGraphics.setBackground(new Color(0, 0, 0, 0));
            } else {
                NodeList children = root.getChildNodes();
                for (int nodeIndex = 0; nodeIndex < children.getLength(); nodeIndex++) {
                    Node nodeItem = children.item(nodeIndex);
                    if (nodeItem.getNodeName().equals("ImageDescriptor")) {
                        NamedNodeMap map = nodeItem.getAttributes();
                        x = Integer.parseInt(map.getNamedItem("imageLeftPosition").getNodeValue());
                        y = Integer.parseInt(map.getNamedItem("imageTopPosition").getNodeValue());
                    }
                }
            }
            masterGraphics.drawImage(image, x, y, null);

            BufferedImage copy = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);
            frames.add(new ImageFrame(copy, delay, disposal));

            if (disposal.equals("restoreToPrevious")) {
                BufferedImage from = null;
                for (int i = frameIndex - 1; i >= 0; i--) {
                    if (!frames.get(i).disposal().equals("restoreToPrevious") || frameIndex == 0) {
                        from = frames.get(i).image();
                        break;
                    }
                }

                master = new BufferedImage(Objects.requireNonNull(from).getColorModel(), from.copyData(null), from.isAlphaPremultiplied(), null);
                masterGraphics = master.createGraphics();
                masterGraphics.setBackground(new Color(0, 0, 0, 0));
            } else if (disposal.equals("restoreToBackgroundColor")) {
                masterGraphics.clearRect(x, y, image.getWidth(), image.getHeight());
            }
        }
        imageReader.dispose();
        return frames;
    }

    private static int fixDelay(int delay) {
        return delay == 0 ? 100 :
                delay < 30 ? delay * 10 :
                delay;
    }
}