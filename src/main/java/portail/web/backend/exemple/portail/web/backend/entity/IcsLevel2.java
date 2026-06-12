package portail.web.backend.exemple.portail.web.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ics_level2")
public class IcsLevel2 extends AbstractLookupEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ics_level1_id", nullable = false)
    private IcsLevel1 icsLevel1;

    public IcsLevel1 getIcsLevel1() {
        return icsLevel1;
    }

    public void setIcsLevel1(IcsLevel1 icsLevel1) {
        this.icsLevel1 = icsLevel1;
    }
}

