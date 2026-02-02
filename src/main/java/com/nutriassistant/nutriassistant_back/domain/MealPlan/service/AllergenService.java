package com.nutriassistant.nutriassistant_back.domain.MealPlan.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Allergen;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.AllergenInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AllergenService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public Optional<Student> findStudentById(Long studentId) {
        return studentRepository.findById(studentId);
    }

    public AllergenInfoResponse toAllergenInfoResponse(Student student) {
        List<Integer> allergenCodes = Allergen.parseCodes(student.getAllergyCodes());
        List<String> allergenNames = new ArrayList<>();

        for (Integer code : allergenCodes) {
            Allergen.fromCode(code)
                    .map(Allergen::getLabel)
                    .ifPresent(allergenNames::add);
        }

        return AllergenInfoResponse.builder()
                .studentId(student.getId())
                .selectedAllergens(allergenCodes)
                .selectedAllergenNames(allergenNames)
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}
