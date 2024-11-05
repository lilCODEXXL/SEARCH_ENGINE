package searchengine.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.persistence.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query(value = "SELECT count(*) FROM lemma where site_id = :id", nativeQuery = true)
    int countById(@Param("id") int id);

    @Query(value = "select * from lemma where lemma in :lemmas AND site_id = :siteId", nativeQuery = true)
    List<Lemma> findLemmaListBySiteTable(@Param("lemmas") List<String> lemmaList,
                                         @Param("siteId") int siteId);

    List<Lemma> findBySiteTableId(int id);
}
