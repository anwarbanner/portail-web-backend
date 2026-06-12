package portail.web.backend.exemple.portail.web.backend.service.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.InvalidPathException;
import java.util.UUID;

@Component
public class NormePdfStorage {

    private final Path baseDir;

    public NormePdfStorage(@Value("${app.storage.norme-pdf-dir:storage/normes}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    public StoredPdf store(Long normeId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("PDF file is required");
        }

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!isPdf(contentType, originalName)) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        ensureDirectory();

        String filename = "norme-" + normeId + "-" + UUID.randomUUID() + ".pdf";
        Path target = resolve(filename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store PDF", ex);
        }

        return new StoredPdf(filename, originalName, contentType, file.getSize());
    }

    public Resource loadAsResource(String pdfPath) {
        Path filePath = resolveExisting(pdfPath);
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Failed to load PDF", ex);
        }
    }

    public void deleteIfExists(String pdfPath) {
        if (pdfPath == null || pdfPath.isBlank()) {
            return;
        }
        Path filePath = resolve(pdfPath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to delete existing PDF", ex);
        }
    }

    private Path resolveExisting(String pdfPath) {
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new ResourceNotFoundException("PDF not available for this norme");
        }
        Path path = resolve(pdfPath);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("PDF not found for this norme");
        }
        return path;
    }

    private Path resolve(String filename) {
        Path path = baseDir.resolve(filename).normalize();
        if (!path.startsWith(baseDir)) {
            throw new BadRequestException("Invalid PDF path");
        }
        return path;
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create PDF storage directory", ex);
        }
    }

    private boolean isPdf(String contentType, String originalName) {
        if (contentType != null && contentType.toLowerCase().startsWith(MediaType.APPLICATION_PDF_VALUE)) {
            return true;
        }
        String name = originalName == null ? "" : originalName.toLowerCase();
        return name.endsWith(".pdf");
    }

    private String normalizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "document.pdf";
        }
        try {
            return Paths.get(originalName).getFileName().toString();
        } catch (InvalidPathException ex) {
            return "document.pdf";
        }
    }

    public record StoredPdf(String path, String originalName, String contentType, long size) {
    }
}
