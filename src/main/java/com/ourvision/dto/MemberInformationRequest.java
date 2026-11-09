package com.ourvision.dto;

import java.time.LocalDate;

public record MemberInformationRequest(String loanStatus, String loanDuration, Integer totalLoansTaken,
		LocalDate joiningDate, String fatherName, String motherName, String maritalStatus, String nationality,
		String languagesKnown, String emergencyContactPerson, String nomineeName, String nomineeRelation) {
}