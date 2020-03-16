package com.redhat.freelancer4j.freelancer.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.redhat.freelancer4j.freelancer.model.Freelancer;
import com.redhat.freelancer4j.freelancer.service.FreelancerService;


@Path("/freelancers")
@Component
public class FreelancerEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(FreelancerEndpoint.class);
    
    @Autowired
    private FreelancerService freelancerService;
    
    @GET
    @Path("/{freelancerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Freelancer getFreelancer(@PathParam("freelancerId") String freelancerId) {
        return freelancerService.getFreelancer(freelancerId);
    }
    
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Freelancer> getFreelancer() {
        return freelancerService.getFreelancers();
    }

}
