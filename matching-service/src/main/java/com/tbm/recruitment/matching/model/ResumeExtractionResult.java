package com.tbm.recruitment.matching.model;

import java.util.List;

public record ResumeExtractionResult(
    List<String> skills,
    double totalYearsExperience,
    EducationLevel highestEducationLevel,
    List<String> jobTitles,
    List<String> domains) {}
