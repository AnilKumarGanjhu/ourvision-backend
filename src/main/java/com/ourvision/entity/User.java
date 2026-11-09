package com.ourvision.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ourvision.enm.BloodGroup;
import com.ourvision.enm.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", indexes = { @Index(name = "idx_users_email", columnList = "email"),
		@Index(name = "idx_users_display_order", columnList = "display_order") })
public class User {

	/* ================= PRIMARY KEY ================= */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // → BIGSERIAL in PG
	private Long id;

	/* ================= BASIC IDENTITY ================= */
	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(length = 25)
	private String phone;

	@Column(nullable = false, length = 255)
	private String password; // BCrypt hash

	/* ================= PERSONAL INFO ================= */
	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth; // → DATE

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", length = 20)
	private Gender gender; // → VARCHAR(20) with CHECK constraint

	@Enumerated(EnumType.STRING)
	@Column(name = "blood_group", length = 10)
	private BloodGroup bloodGroup; // → VARCHAR(10)

	/* ================= PROFESSIONAL INFO ================= */
	@Column(name = "designation", length = 100)
	private String designation;

	@Column(name = "short_bio", length = 200)
	private String shortBio;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description; // → TEXT

	// JSONB for PostgreSQL — indexable & queryable
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "skills", columnDefinition = "jsonb")
	private String skills; // JSON string: ["React","Node"]

	/* ================= PHOTO ================= */
	@Column(length = 500)
	private String avatar;

	@Column(name = "image_url", columnDefinition = "TEXT")
	private String imageUrl;

	/* ================= CONTACT / SOCIAL ================= */
	@Column(name = "linkedin_url", length = 300)
	private String linkedinUrl;

	@Column(name = "github_url", length = 300)
	private String githubUrl;

	@Column(name = "address", columnDefinition = "TEXT")
	private String address;

	@Column(name = "aadhar_number", length = 255)
	private String aadharNumber; // ⚠️ encrypt before save

	/* ================= MEMBER INFORMATION ================= */
	@Column(name = "loan_status", length = 20)
	private String loanStatus = "CLEAR";

	@Column(name = "loan_duration", length = 100)
	private String loanDuration;

	@Column(name = "total_loans_taken")
	private Integer totalLoansTaken = 0;

	@Column(name = "joining_date")
	private LocalDate joiningDate;

	@Column(name = "father_name", length = 150)
	private String fatherName;

	@Column(name = "mother_name", length = 150)
	private String motherName;

	@Column(name = "marital_status", length = 30)
	private String maritalStatus;

	@Column(name = "nationality", length = 100)
	private String nationality;

	@Column(name = "languages_known", columnDefinition = "TEXT")
	private String languagesKnown;

	@Column(name = "emergency_contact_person", length = 150)
	private String emergencyContactPerson;

	/* ================= SYSTEM / CONTROL ================= */
	@Column(nullable = false, length = 20)
	private String status = "ACTIVE";

	@Column(name = "display_order")
	private Integer displayOrder = 0;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_role"))
	private Role role;

	// PostgreSQL: TIMESTAMPTZ → OffsetDateTime (best practice)
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@Column(name = "nominee_name", length = 150)
	private String nomineeName;

	@Column(name = "nominee_relation", length = 100)
	private String nomineeRelation;

	@Column(name = "member_photo_url", columnDefinition = "TEXT")
	private String memberPhotoUrl;

	/* ================= CONSTRUCTORS ================= */
	public User() {
	}

	/* ================= LIFECYCLE HOOKS ================= */
	@PreUpdate
	public void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	@PrePersist
	public void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = OffsetDateTime.now();
		}
		this.updatedAt = OffsetDateTime.now();
	}

	/* ================= GETTERS / SETTERS ================= */

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public BloodGroup getBloodGroup() {
		return bloodGroup;
	}

	public void setBloodGroup(BloodGroup bloodGroup) {
		this.bloodGroup = bloodGroup;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getShortBio() {
		return shortBio;
	}

	public void setShortBio(String shortBio) {
		this.shortBio = shortBio;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSkills() {
		return skills;
	}

	public void setSkills(String skills) {
		this.skills = skills;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getLinkedinUrl() {
		return linkedinUrl;
	}

	public void setLinkedinUrl(String linkedinUrl) {
		this.linkedinUrl = linkedinUrl;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAadharNumber() {
		return aadharNumber;
	}

	public void setAadharNumber(String aadharNumber) {
		this.aadharNumber = aadharNumber;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getLoanStatus() {
		return loanStatus;
	}

	public void setLoanStatus(String loanStatus) {
		this.loanStatus = loanStatus;
	}

	public String getLoanDuration() {
		return loanDuration;
	}

	public void setLoanDuration(String loanDuration) {
		this.loanDuration = loanDuration;
	}

	public Integer getTotalLoansTaken() {
		return totalLoansTaken;
	}

	public void setTotalLoansTaken(Integer totalLoansTaken) {
		this.totalLoansTaken = totalLoansTaken;
	}

	public LocalDate getJoiningDate() {
		return joiningDate;
	}

	public void setJoiningDate(LocalDate joiningDate) {
		this.joiningDate = joiningDate;
	}

	public String getFatherName() {
		return fatherName;
	}

	public void setFatherName(String fatherName) {
		this.fatherName = fatherName;
	}

	public String getMotherName() {
		return motherName;
	}

	public void setMotherName(String motherName) {
		this.motherName = motherName;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public String getNationality() {
		return nationality;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public String getLanguagesKnown() {
		return languagesKnown;
	}

	public void setLanguagesKnown(String languagesKnown) {
		this.languagesKnown = languagesKnown;
	}

	public String getEmergencyContactPerson() {
		return emergencyContactPerson;
	}

	public void setEmergencyContactPerson(String emergencyContactPerson) {
		this.emergencyContactPerson = emergencyContactPerson;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	// Getter aur Setter bhi generate kar lena
	public String getNomineeName() {
		return nomineeName;
	}

	public void setNomineeName(String nomineeName) {
		this.nomineeName = nomineeName;
	}

	public String getNomineeRelation() {
		return nomineeRelation;
	}

	public void setNomineeRelation(String nomineeRelation) {
		this.nomineeRelation = nomineeRelation;
	}

	public String getMemberPhotoUrl() {
		return memberPhotoUrl;
	}

	public void setMemberPhotoUrl(String memberPhotoUrl) {
		this.memberPhotoUrl = memberPhotoUrl;
	}
}