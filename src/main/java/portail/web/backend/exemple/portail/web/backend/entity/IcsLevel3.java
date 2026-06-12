package portail.web.backend.exemple.portail.web.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ics_level3")
public class IcsLevel3 extends AbstractLookupEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ics_level2_id", nullable = false)
    private IcsLevel2 icsLevel2;

    public IcsLevel2 getIcsLevel2() {
        return icsLevel2;
    }

    public void setIcsLevel2(IcsLevel2 icsLevel2) {
        this.icsLevel2 = icsLevel2;
    }
}

