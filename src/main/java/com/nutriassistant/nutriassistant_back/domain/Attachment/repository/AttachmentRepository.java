package com.nutriassistant.nutriassistant_back.domain.Attachment.repository;

import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.Attachment;
import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.RelatedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByRelatedTypeAndRelatedId(RelatedType relatedType, Long relatedId);

    boolean existsByRelatedTypeAndRelatedId(RelatedType relatedType, Long relatedId);
}
