package com.barn.barn.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 图片处理Controller - 缩略图生成
 * 将原图按比例缩小并缓存到磁盘，后续请求直接读缓存
 */
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Value("${upload.path:d:/云盘/MyWeb/烤房项目/upload/}")
    private String uploadPath;

    /**
     * 缩略图生成接口
     *
     * @param path 原图路径（数据库中存储的路径，如 upload/xxx.jpg 或 /upload/xxx.jpg 或 xxx.jpg）
     * @param size 目标尺寸（正方形边长，默认200px）
     */
    @GetMapping("/thumb")
    public ResponseEntity<byte[]> thumbnail(
            @RequestParam String path,
            @RequestParam(defaultValue = "200") int size) {

        // 限制尺寸范围，防止恶意请求
        if (size < 50) size = 50;
        if (size > 800) size = 800;

        // 解析原图文件
        File originalFile = resolveOriginalFile(path);
        if (originalFile == null || !originalFile.exists() || !originalFile.isFile()) {
            return ResponseEntity.notFound().build();
        }

        // 缩略图缓存目录
        File thumbDir = new File(originalFile.getParentFile(), ".thumbs");
        if (!thumbDir.exists()) {
            thumbDir.mkdirs();
        }

        // 缓存文件名：原名_size.jpg（统一转 jpg 减小体积）
        String originalName = originalFile.getName();
        int dotIdx = originalName.lastIndexOf('.');
        String baseName = dotIdx > 0 ? originalName.substring(0, dotIdx) : originalName;
        File thumbFile = new File(thumbDir, baseName + "_" + size + ".jpg");

        // 如果缓存已存在且比原图新，直接返回缓存
        if (thumbFile.exists() && thumbFile.lastModified() >= originalFile.lastModified()) {
            return serveImage(thumbFile);
        }

        // 生成缩略图
        try {
            BufferedImage original = ImageIO.read(originalFile);
            if (original == null) {
                return ResponseEntity.notFound().build();
            }

            int origW = original.getWidth();
            int origH = original.getHeight();

            // 按比例缩放，取较短边
            int targetW, targetH;
            if (origW > origH) {
                targetH = size;
                targetW = (int) ((double) origW / origH * size);
            } else {
                targetW = size;
                targetH = (int) ((double) origH / origW * size);
            }

            // 高质量缩放
            BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, targetW, targetH, null);
            g2d.dispose();

            // 写入缓存文件（JPEG 格式）
            ImageIO.write(scaled, "jpg", thumbFile);

            return serveImage(thumbFile);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 将数据库中存储的图片路径解析为磁盘文件
     * 处理各种格式：upload/xxx.jpg、/upload/xxx.jpg、xxx.jpg、http://...
     */
    private File resolveOriginalFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String cleaned = path.trim();

        // 完整 URL（http/https 开头）- 不处理外部链接
        if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return null;
        }

        // 去掉前导斜杠
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        // 去掉 "upload/" 前缀
        if (cleaned.startsWith("upload/")) {
            cleaned = cleaned.substring("upload/".length());
        }

        // 仅允许图片扩展名，并在规范化绝对路径后确认仍位于上传目录内。
        int dot = cleaned.lastIndexOf('.');
        if (dot <= 0 || dot == cleaned.length() - 1) {
            return null;
        }
        String extension = cleaned.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            return null;
        }

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path candidate = basePath.resolve(cleaned).normalize().toAbsolutePath();
        if (!candidate.startsWith(basePath)) {
            return null;
        }
        return candidate.toFile();
    }

    /**
     * 返回图片文件，设置长缓存
     */
    private ResponseEntity<byte[]> serveImage(File imageFile) {
        try {
            byte[] data = Files.readAllBytes(imageFile.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
