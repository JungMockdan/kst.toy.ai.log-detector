package kst.toy.ai.logdetector.repository;

import kst.toy.ai.logdetector.domain.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LogRepository extends JpaRepository<AccessLog, Long> {

    @Query("SELECT DISTINCT l.ip FROM AccessLog l")
    List<String> findDistinctIps();

    List<AccessLog> findByIp(String ip);
}