package searchengine.persistence.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  Site {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private Status status;

    @NonNull
    @Column(name = "status_time", updatable = true, columnDefinition = "DATETIME NOT NULL")
    @Builder.Default
    private LocalDateTime statusTime = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @NonNull
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;

    @NonNull
    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

    @OneToMany(mappedBy = "siteId", fetch = FetchType.LAZY)
    private List<Page> pages = new ArrayList<>();

    @OneToMany(mappedBy = "siteTable", fetch = FetchType.LAZY)
    private List<Lemma> lemmaTables = new ArrayList<>();

    public String getLastError() {
        return lastError == null ? "" : lastError;
    }

    @PreUpdate
    void update() {
        statusTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SitePage{" +
                "id=" + id +
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
