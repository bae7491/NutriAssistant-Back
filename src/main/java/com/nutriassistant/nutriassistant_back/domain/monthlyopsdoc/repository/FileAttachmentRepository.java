package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.repository;

import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    // [설명] relatedType("OPS")과 relatedId(운영자료 ID)를 기준으로 파일 목록을 조회합니다.
    List<FileAttachment> findAllByRelatedTypeAndRelatedId(String relatedType, Long relatedId);
}