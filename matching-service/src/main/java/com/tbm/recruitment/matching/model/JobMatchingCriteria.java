package com.tbm.recruitment.matching.model;

import java.util.List;

public record JobMatchingCriteria(
    String title,
    List<String> requiredSkills,
    int minimumYearsExperience,
    EducationLevel requiredEducationLevel,
    String domain) {}
