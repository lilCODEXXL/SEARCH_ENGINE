package searchengine.persistence.model;


import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;


@Entity
@Table(name = "`index`")
@Data
@Builder
@NoArgsConstructor
public class Index implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false,
            foreignKey=@ForeignKey(name = "FK_index_page"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false,
            foreignKey=@ForeignKey(name = "FK_index_lemma"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lemma lemma;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    public Index(int id, Page page, Lemma lemma, float rank) {
        this.id = id;
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}
