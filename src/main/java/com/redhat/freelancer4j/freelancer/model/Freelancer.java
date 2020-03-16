package com.redhat.freelancer4j.freelancer.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "freelancer")
@NamedQuery(name="Freelancer.findAll", query="SELECT f FROM Freelancer f") // use Freelancer instead of freelancer
public class Freelancer implements Serializable {

    private static final long serialVersionUID = 6964558044240061049L;

    @Id
    private String freelancerId;
    private String firstName;
    private String lastName;
    private String email;
    @Type(type = "com.redhat.freelancer4j.freelancer.service.GenericArrayUserType")
    private String[] skills ;
    //private List<String> skills ;
    

    public Freelancer() {
    }

    public Freelancer(String freelancerId, String firstName, String lastName, String email, String[] skills) {
    //public Freelancer(String freelancerId, String firstName, String lastName, String email, List<String> skills) {
	//public Freelancer(String freelancerId, String firstName, String lastName, String email) {
        super();
        this.freelancerId = freelancerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.skills = skills;
    }

    public String getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(String freelancerId) {
        this.freelancerId = freelancerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    //public List<String> getSkills() {
	public String[] getSkills() {
        return skills;
    }
    
    //public void setSkills(List<String> skills) {
	public void setSkills(String[] skills) {
        this.skills = skills;
    }
}
