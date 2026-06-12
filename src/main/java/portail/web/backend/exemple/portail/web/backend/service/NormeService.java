package portail.web.backend.exemple.portail.web.backend.service;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import portail.web.backend.exemple.portail.web.backend.dto.NormeRequest;
import portail.web.backend.exemple.portail.web.backend.dto.NormeResponse;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.mapper.NormeMapper;
import portail.web.backend.exemple.portail.web.backend.repository.NormeRepository;
import portail.web.backend.exemple.portail.web.backend.service.support.NormePdfStorage;
import portail.web.backend.exemple.portail.web.backend.service.support.NormeRelationResolver;

@Service
public class NormeService {

    private final NormeRepository normeRepository;
    private final NormeRelationResolver normeRelationResolver;
    private final NormeMapper normeMapper;
    private final NormePdfStorage normePdfStorage;

    public NormeService(NormeRepository normeRepository,
                        NormeRelationResolver normeRelationResolver,
                        NormeMapper normeMapper,
                        NormePdfStorage normePdfStorage) {
        this.normeRepository = normeRepository;
        this.normeRelationResolver = normeRelationResolver;
        this.normeMapper = normeMapper;
        this.normePdfStorage = normePdfStorage;
    }

    public Page<NormeResponse> findAll(String search, String reference, Long statutId, Long icsLevel1Id, Long icsLevel2Id, Long icsLevel3Id, Pageable pageable) {
        Specification<Norme> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("reference")), pattern),
                    cb.like(cb.lower(root.get("titreFr")), pattern),
                    cb.like(cb.lower(root.get("titreEn")), pattern),
                    cb.like(cb.lower(root.get("titreDe")), pattern)
            ));
        }

        if (reference != null && !reference.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("reference")), "%" + reference.toLowerCase() + "%"));
        }

        if (statutId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("statut", JoinType.LEFT).get("id"), statutId));
        }

        if (icsLevel1Id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("icsLevel1", JoinType.LEFT).get("id"), icsLevel1Id));
        }

        if (icsLevel2Id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("icsLevel2", JoinType.LEFT).get("id"), icsLevel2Id));
        }

        if (icsLevel3Id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("icsLevel3", JoinType.LEFT).get("id"), icsLevel3Id));
        }

        return normeRepository.findAll(spec, pageable).map(normeMapper::toResponse);
    }

    public NormeResponse findById(Long id) {
        Norme norme = normeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Norme not found with id=" + id));
        return normeMapper.toResponse(norme);
    }

    @Transactional
    public NormeResponse create(NormeRequest request) {
        if (normeRepository.existsByReference(request.reference())) {
            throw new BadRequestException("Norme reference already exists: " + request.reference());
        }
        Norme norme = new Norme();
        applyRequest(norme, request);
        return normeMapper.toResponse(normeRepository.save(norme));
    }

    @Transactional
    public NormeResponse update(Long id, NormeRequest request) {
        Norme norme = normeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Norme not found with id=" + id));

        if (!norme.getReference().equals(request.reference()) && normeRepository.existsByReference(request.reference())) {
            throw new BadRequestException("Norme reference already exists: " + request.reference());
        }

        applyRequest(norme, request);
        return normeMapper.toResponse(normeRepository.save(norme));
    }

    @Transactional
    public void delete(Long id) {
        Norme norme = normeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Norme not found with id=" + id));
        String pdfPath = norme.getPdfPath();
        normeRepository.delete(norme);
        normePdfStorage.deleteIfExists(pdfPath);
    }

    @Transactional
    public NormeResponse uploadPdf(Long id, MultipartFile file) {
        Norme norme = normeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Norme not found with id=" + id));

        NormePdfStorage.StoredPdf stored = normePdfStorage.store(id, file);
        String previousPath = norme.getPdfPath();

        try {
            norme.setPdfPath(stored.path());
            norme.setPdfOriginalName(stored.originalName());
            norme.setPdfContentType(stored.contentType());
            norme.setPdfSize(stored.size());
            Norme saved = normeRepository.save(norme);
            try {
                normePdfStorage.deleteIfExists(previousPath);
            } catch (RuntimeException ex) {
                normePdfStorage.deleteIfExists(stored.path());
                throw ex;
            }
            return normeMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            normePdfStorage.deleteIfExists(stored.path());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public NormePdfDownload downloadPdf(Long id) {
        Norme norme = normeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Norme not found with id=" + id));
        return new NormePdfDownload(
                normePdfStorage.loadAsResource(norme.getPdfPath()),
                norme.getPdfOriginalName(),
                norme.getPdfContentType(),
                norme.getPdfSize()
        );
    }

    private void applyRequest(Norme norme, NormeRequest request) {
        norme.setReference(request.reference());
        norme.setPublicationDate(request.publicationDate());
        norme.setTitreFr(request.titreFr());
        norme.setTitreEn(request.titreEn());
        norme.setTitreDe(request.titreDe());
        norme.setDescripteurFr(request.descripteurFr());
        norme.setDescripteurEn(request.descripteurEn());
        norme.setDocumentIdentifier(request.documentIdentifier());
        if (request.includedInSubscription() != null) {
            norme.setIncludedInSubscription(request.includedInSubscription());
        }
        norme.setAfnorIndex(request.afnorIndex());
        norme.setPrintNumber(request.printNumber());
        norme.setPrintDate(request.printDate());
        if (request.mandatory() != null) {
            norme.setMandatory(request.mandatory());
        }
        norme.setRegulationSpecifique(request.regulationSpecifique());
        normeRelationResolver.apply(norme, request);
    }
}


