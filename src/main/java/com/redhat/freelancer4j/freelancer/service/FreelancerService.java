package com.redhat.freelancer4j.freelancer.service;

import java.util.List;

import com.redhat.freelancer4j.freelancer.model.Freelancer;

public interface FreelancerService {

    List<Freelancer> getFreelancers();

    Freelancer getFreelancer(String freelancerId);

}