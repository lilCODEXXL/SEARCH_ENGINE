package searchengine.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.persistence.model.Page;
import searchengine.persistence.model.Site;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    int countBySiteId(Site siteId);

    @Query(value = "SELECT * FROM page WHERE site_id = :id", nativeQuery = true)
    List<Page> findAllBySiteId(@Param("id") int id);

    @Query(value = "SELECT p.* FROM page p JOIN `index` i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas", nativeQuery = true)
    List<Page> findIdsByLemmaList(@Param("lemmas") List<Integer> lemmas);
}
