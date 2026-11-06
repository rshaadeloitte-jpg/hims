package com.hims.reporting.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.sql.Date;
import java.util.Objects;

@Entity
@Table(name = "rm_op_stats")
public class RmOpStats {

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "stat_date") private Date statDate;
        @Column(name = "op_type") private String opType;
        public Key() {}
        public Key(Date statDate, String opType) { this.statDate = statDate; this.opType = opType; }
        public Date getStatDate() { return statDate; }
        public void setStatDate(Date v) { this.statDate = v; }
        public String getOpType() { return opType; }
        public void setOpType(String v) { this.opType = v; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(statDate, k.statDate) && Objects.equals(opType, k.opType);
        }
        @Override public int hashCode() { return Objects.hash(statDate, opType); }
    }

    @EmbeddedId private Key id;
    @Column(name = "cnt", nullable = false) private Integer cnt = 0;
    public RmOpStats() {}
    public RmOpStats(Date statDate, String opType) { this.id = new Key(statDate, opType); }
    public Key getId() { return id; }
    public void setId(Key v) { this.id = v; }
    public Integer getCnt() { return cnt; }
    public void setCnt(Integer v) { this.cnt = v; }
}
