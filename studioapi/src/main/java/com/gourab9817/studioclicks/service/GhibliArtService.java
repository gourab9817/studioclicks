package com.gourab9817.studioclicks.service;

import com.gourab9817.studioclicks.client.StabilityAIClient;
import com.gourab9817.studioclicks.dto.TextToImageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

// Custom MultipartFile implementation for in-memory usage
class InMemoryMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getOriginalFilename() { return originalFilename; }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public boolean isEmpty() { return content == null || content.length == 0; }

    @Override
    public long getSize() { return content.length; }

    @Override
    public byte[] getBytes() { return content; }

    @Override
    public ByteArrayInputStream getInputStream() { return new ByteArrayInputStream(content); }

    @Override
    public void transferTo(File dest) throws IOException {
        java.nio.file.Files.write(dest.toPath(), content);
    }
}

@Service
public class GhibliArtService {

    private final StabilityAIClient stabilityAIClient;
    private final String apiKey;

    public GhibliArtService(StabilityAIClient stabilityAIClient, @Value("${stability.api.key}") String apiKey) {
        this.stabilityAIClient = stabilityAIClient;
        this.apiKey = apiKey;
    }

    /**
     * Generate Ghibli-style art from an uploaded image
     * @param image uploaded image
     * @param prompt optional prompt to modify the style
     * @return byte array of generated image
     */
    public byte[] createGhibliArt(MultipartFile image, String prompt) {
        try {
            // Read original image
            BufferedImage originalImage = ImageIO.read(image.getInputStream());

            // Resize to valid dimension 1024x1024
            BufferedImage resizedImage = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, 1024, 1024, null);
            g.dispose();

            // Convert resized image to in-memory MultipartFile
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            MultipartFile fileForFeign = new InMemoryMultipartFile(
                    "image",
                    image.getOriginalFilename(),
                    image.getContentType(),
                    baos.toByteArray()
            );

            // Construct final prompt
            String finalPrompt;
            if (prompt == null || prompt.isBlank()) {
                // Default: naturalistic cute Ghibli-style image
                finalPrompt = "cinematic style with pencil stroke image seems like a sketch ";
            } else {
                // User provided prompt: preserve the original image features + prompt
                finalPrompt = prompt ;
            }

            String engineId = "stable-diffusion-xl-1024-v1-0";
            String stylePreset = "cinematic";

            return stabilityAIClient.generateImageFromImage(
                    "Bearer " + apiKey,
                    engineId,
                    fileForFeign,
                    finalPrompt,
                    stylePreset
            );

        } catch (IOException e) {
            throw new RuntimeException("Error processing the uploaded image", e);
        }
    }

    /**
     * Generate Ghibli-style art from text prompt only
     * @param prompt optional prompt
     * @param style style parameter ("general" or other)
     * @return byte array of generated image
     */
    public byte[] createGhibliArtFromText(String prompt, String style) {
        String finalPrompt;
        if (prompt == null || prompt.isBlank()) {
            // Default cute Ghibli-style image
            finalPrompt = "A cute, naturalistic Studio Ghibli-style illustration, bright, magical, detailed, soft colors, charming scene";
        } else {
            finalPrompt = prompt + ", in the beautiful, detailed Studio Ghibli anime style, soft colors, magical atmosphere";
        }

        String engineId = "stable-diffusion-xl-1024-v1-0";
        String stylePreset = style.equals("general") ? "cinematic" : style.replace("_", "-");

        TextToImageRequest requestPayload = new TextToImageRequest(finalPrompt, stylePreset);

        return stabilityAIClient.generateImageFromText(
                "Bearer " + apiKey,
                engineId,
                requestPayload
        );
    }
}

//analog-film, anime, cinematic, comic-book, digital-art, enhance, fantasy-art, isometric, line-art, low-poly, modeling-compound, neon-punk, origami, photographic, pixel-art, 3d-model, tile-texture.