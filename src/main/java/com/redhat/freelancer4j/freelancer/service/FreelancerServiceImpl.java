package com.redhat.freelancer4j.freelancer.service;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redhat.freelancer4j.freelancer.model.Freelancer;

@Component
public class FreelancerServiceImpl implements FreelancerService {
	@Autowired
	private EntityManager em;

	@Override
	public List<Freelancer> getFreelancers() {
		List<Freelancer> freelancers = em.createNamedQuery("Freelancer.findAll", Freelancer.class).getResultList();
		return freelancers;
	}
	
	@Override
	public Freelancer getFreelancer(String id) {
		Freelancer freelancer = em.find(Freelancer.class, id);
		return freelancer;
	}
}