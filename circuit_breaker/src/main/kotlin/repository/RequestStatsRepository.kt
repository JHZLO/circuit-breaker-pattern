package com.jhzlo.repository

import com.jhzlo.entity.RequestStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RequestStatsRepository : JpaRepository<RequestStats, Long> {
}
