package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.School;

import java.time.LocalDateTime;

public class SchoolResponse {

    @JsonProperty("school_id")
    private Long schoolId;

    @JsonProperty("dietitian_id")
    private Long dietitianId;

    @JsonProperty("school_name")
    private String schoolName;

    @JsonProperty("school_type")
    private String schoolType;

    private String phone;
    private String email;

    @JsonProperty("student_count")
    private Integer studentCount;

    @JsonProperty("target_unit_price")
    private Integer targetUnitPrice;

    @JsonProperty("max_unit_price")
    private Integer maxUnitPrice;

    @JsonProperty("operation_rules")
    private String operationRules;

    @JsonProperty("cook_workers")
    private Integer cookWorkers;

    @JsonProperty("kitchen_equipment")
    private String kitchenEquipment;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public SchoolResponse() {}

    public SchoolResponse(School school) {
        this.schoolId = school.getId();
        this.dietitianId = school.getDietitian().getId();
        this.schoolName = school.getSchoolName();
        this.schoolType = school.getSchoolType();
        this.phone = school.getPhone();
        this.email = school.getEmail();
        this.studentCount = school.getStudentCount();
        this.targetUnitPrice = school.getTargetUnitPrice();
        this.maxUnitPrice = school.getMaxUnitPrice();
        this.operationRules = school.getOperationRules();
        this.cookWorkers = school.getCookWorkers();
        this.kitchenEquipment = school.getKitchenEquipment();
        this.createdAt = school.getCreatedAt();
        this.updatedAt = school.getUpdatedAt();
    }

    public Long getSchoolId() { return schoolId; }
    public Long getDietitianId() { return dietitianId; }
    public String getSchoolName() { return schoolName; }
    public String getSchoolType() { return schoolType; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Integer getStudentCount() { return studentCount; }
    public Integer getTargetUnitPrice() { return targetUnitPrice; }
    public Integer getMaxUnitPrice() { return maxUnitPrice; }
    public String getOperationRules() { return operationRules; }
    public Integer getCookWorkers() { return cookWorkers; }
    public String getKitchenEquipment() { return kitchenEquipment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
