package portail.web.backend.exemple.portail.web.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_types")
public class DocumentType extends AbstractLookupEntity {
}

