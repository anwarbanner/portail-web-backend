package portail.web.backend.exemple.portail.web.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import portail.web.backend.exemple.portail.web.backend.common.entity.BaseTimestampEntity;

import java.time.LocalDate;

@Entity
@Table(name = "normes")
public class Norme extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String reference;

    private LocalDate publicationDate;

    @Column(length = 255)
    private String titreFr;

    @Column(length = 255)
    private String titreEn;

    @Column(length = 255)
    private String titreDe;

    @Column(columnDefinition = "TEXT")
    private String descripteurFr;

    @Column(columnDefinition = "TEXT")
    private String descripteurEn;

    @Column(length = 120)
    private String documentIdentifier;

    @Column(nullable = false)
    private boolean includedInSubscription;

    @Column(length = 80)
    private String afnorIndex;

    @Column(length = 80)
    private String printNumber;

    private LocalDate printDate;

    @Column(nullable = false)
    private boolean mandatory;

    @Column(columnDefinition = "TEXT")
    private String regulationSpecifique;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "pdf_original_name", length = 255)
    private String pdfOriginalName;

    @Column(name = "pdf_content_type", length = 120)
    private String pdfContentType;

    @Column(name = "pdf_size")
    private Long pdfSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id")
    private Statut statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private NormeCollection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industrial_branch_id")
    private IndustrialBranch industrialBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_family_id")
    private ProductFamily productFamily;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_family_id")
    private SubFamily subFamily;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter1_id")
    private Filter1 filter1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ics_level1_id")
    private IcsLevel1 icsLevel1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ics_level2_id")
    private IcsLevel2 icsLevel2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ics_level3_id")
    private IcsLevel3 icsLevel3;

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getTitreFr() {
        return titreFr;
    }

    public void setTitreFr(String titreFr) {
        this.titreFr = titreFr;
    }

    public String getTitreEn() {
        return titreEn;
    }

    public void setTitreEn(String titreEn) {
        this.titreEn = titreEn;
    }

    public String getTitreDe() {
        return titreDe;
    }

    public void setTitreDe(String titreDe) {
        this.titreDe = titreDe;
    }

    public String getDescripteurFr() {
        return descripteurFr;
    }

    public void setDescripteurFr(String descripteurFr) {
        this.descripteurFr = descripteurFr;
    }

    public String getDescripteurEn() {
        return descripteurEn;
    }

    public void setDescripteurEn(String descripteurEn) {
        this.descripteurEn = descripteurEn;
    }

    public String getDocumentIdentifier() {
        return documentIdentifier;
    }

    public void setDocumentIdentifier(String documentIdentifier) {
        this.documentIdentifier = documentIdentifier;
    }

    public boolean isIncludedInSubscription() {
        return includedInSubscription;
    }

    public void setIncludedInSubscription(boolean includedInSubscription) {
        this.includedInSubscription = includedInSubscription;
    }

    public String getAfnorIndex() {
        return afnorIndex;
    }

    public void setAfnorIndex(String afnorIndex) {
        this.afnorIndex = afnorIndex;
    }

    public String getPrintNumber() {
        return printNumber;
    }

    public void setPrintNumber(String printNumber) {
        this.printNumber = printNumber;
    }

    public LocalDate getPrintDate() {
        return printDate;
    }

    public void setPrintDate(LocalDate printDate) {
        this.printDate = printDate;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getRegulationSpecifique() {
        return regulationSpecifique;
    }

    public void setRegulationSpecifique(String regulationSpecifique) {
        this.regulationSpecifique = regulationSpecifique;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getPdfOriginalName() {
        return pdfOriginalName;
    }

    public void setPdfOriginalName(String pdfOriginalName) {
        this.pdfOriginalName = pdfOriginalName;
    }

    public String getPdfContentType() {
        return pdfContentType;
    }

    public void setPdfContentType(String pdfContentType) {
        this.pdfContentType = pdfContentType;
    }

    public Long getPdfSize() {
        return pdfSize;
    }

    public void setPdfSize(Long pdfSize) {
        this.pdfSize = pdfSize;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public NormeCollection getCollection() {
        return collection;
    }

    public void setCollection(NormeCollection collection) {
        this.collection = collection;
    }

    public IndustrialBranch getIndustrialBranch() {
        return industrialBranch;
    }

    public void setIndustrialBranch(IndustrialBranch industrialBranch) {
        this.industrialBranch = industrialBranch;
    }

    public ProductFamily getProductFamily() {
        return productFamily;
    }

    public void setProductFamily(ProductFamily productFamily) {
        this.productFamily = productFamily;
    }

    public SubFamily getSubFamily() {
        return subFamily;
    }

    public void setSubFamily(SubFamily subFamily) {
        this.subFamily = subFamily;
    }

    public Filter1 getFilter1() {
        return filter1;
    }

    public void setFilter1(Filter1 filter1) {
        this.filter1 = filter1;
    }

    public IcsLevel1 getIcsLevel1() {
        return icsLevel1;
    }

    public void setIcsLevel1(IcsLevel1 icsLevel1) {
        this.icsLevel1 = icsLevel1;
    }

    public IcsLevel2 getIcsLevel2() {
        return icsLevel2;
    }

    public void setIcsLevel2(IcsLevel2 icsLevel2) {
        this.icsLevel2 = icsLevel2;
    }

    public IcsLevel3 getIcsLevel3() {
        return icsLevel3;
    }

    public void setIcsLevel3(IcsLevel3 icsLevel3) {
        this.icsLevel3 = icsLevel3;
    }
}

