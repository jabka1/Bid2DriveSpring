package team.bid2drivespring.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.Report;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByStatus(Report.ReportStatus status);
    Page<Report> findByTypeOrderByStatusAscIdAsc(Report.ReportType type, Pageable pageable);
    Page<Report> findByTypeAndStatus(Report.ReportType type, Report.ReportStatus status, Pageable pageable);
    void deleteAllByReportedAuction(Auction auction);
    boolean existsByReportedAuction(Auction auction);
}
