package com.nutriassistant.nutriassistant_back.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SchoolRequest {

    @NotBlank
    @JsonProperty("school_name")
    private String schoolName;

    @NotBlank
    @JsonProperty("school_type")
    private String schoolType; // 초/중/고

    @NotBlank
    private String phone;

    @NotBlank
    private String email;

    @NotNull @Min(0)
    @JsonProperty("student_count")
    private Integer studentCount;

    @NotNull @Min(0)
    @JsonProperty("target_unit_price")
    private Integer targetUnitPrice;

    @NotNull @Min(0)
    @JsonProperty("max_unit_price")
    private Integer maxUnitPrice;

    @JsonProperty("operation_rules")
    private String operationRules;

    @NotNull @Min(0)
    @JsonProperty("cook_workers")
    private Integer cookWorkers;

    @JsonProperty("kitchen_equipment")
    private String kitchenEquipment;

    public SchoolRequest() {}

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getSchoolType() { return schoolType; }
    public void setSchoolType(String schoolType) { this.schoolType = schoolType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }

    public Integer getTargetUnitPrice() { return targetUnitPrice; }
    public void setTargetUnitPrice(Integer targetUnitPrice) { this.targetUnitPrice = targetUnitPrice; }

    public Integer getMaxUnitPrice() { return maxUnitPrice; }
    public void setMaxUnitPrice(Integer maxUnitPrice) { this.maxUnitPrice = maxUnitPrice; }

    public String getOperationRules() { return operationRules; }
    public void setOperationRules(String operationRules) { this.operationRules = operationRules; }

    public Integer getCookWorkers() { return cookWorkers; }
    public void setCookWorkers(Integer cookWorkers) { this.cookWorkers = cookWorkers; }

    public String getKitchenEquipment() { return kitchenEquipment; }
    public void setKitchenEquipment(String kitchenEquipment) { this.kitchenEquipment = kitchenEquipment; }
}
