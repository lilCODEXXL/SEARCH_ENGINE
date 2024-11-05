package searchengine.persistence.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "lemma")
@Data
@Builder
@NoArgsConstructor
public class Lemma implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private float frequency;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false, foreignKey = @ForeignKey(name = "FK_lemma_site"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site siteTable;

    public Lemma(int id, String lemma, float frequency, Site site) {
        this.id = id;
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteTable = site;
    }
}
