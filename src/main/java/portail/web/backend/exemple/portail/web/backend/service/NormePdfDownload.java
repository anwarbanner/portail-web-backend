package portail.web.backend.exemple.portail.web.backend.service;

import org.springframework.core.io.Resource;

public record NormePdfDownload(Resource resource, String filename, String contentType, Long size) {
}
