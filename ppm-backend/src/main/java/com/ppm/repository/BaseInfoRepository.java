package com.ppm.repository;

import com.ppm.entity.BaseInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 基地主数据仓储。
 */
public interface BaseInfoRepository extends JpaRepository<BaseInfo, Long> {

    Optional<BaseInfo> findByBaseCode(String baseCode);

    /** 按基地编码是否存在 */
    boolean existsByBaseCode(String baseCode);

    /** 排除指定 id，按基地编码是否存在（用于更新时校验） */
    boolean existsByBaseCodeAndIdNot(String baseCode, Long id);

    /** 批量按基地编码查询 */
    List<BaseInfo> findByBaseCodeIn(Collection<String> baseCodes);

    /** 按编码排序查询全部 */
    List<BaseInfo> findAllByOrderByBaseCodeAsc();
}
