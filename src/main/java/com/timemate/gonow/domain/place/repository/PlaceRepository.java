package com.timemate.gonow.domain.place.repository;

import com.timemate.gonow.domain.place.constant.PlaceType;
import com.timemate.gonow.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    // 전체 목록 조회 (type=null 일 때)
    @Query("SELECT p FROM Place p WHERE p.member.id = :memberId ORDER BY p.updatedAt DESC")
    List<Place> findAllByMember(@Param("memberId") Long memberId);

    // 특정 목록 조회 (type 필터링)
    @Query("SELECT p FROM Place p WHERE p.member.id = :memberId AND p.placeType = :placeType ORDER BY p.updatedAt DESC")
    List<Place> findAllByMemberAndType(@Param("memberId") Long memberId, @Param("placeType") PlaceType placeType);

    // Upsert용 조회 메서드 (동일 주소 중복 확인용)
    @Query("SELECT p FROM Place p WHERE p.member.id = :memberId AND p.location.address = :address")
    Optional<Place> findByMemberAndAddress(@Param("memberId") Long memberId, @Param("address") String address);

    // 삭제 전 소유자 확인용
    Optional<Place> findByIdAndMemberId(Long placeId, Long memberId);
}
