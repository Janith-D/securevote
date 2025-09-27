package com.securevote.securevotebackend.Service;

import com.securevote.securevotebackend.Dto.ElectionResponseDto;
import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Entity.User;
import com.securevote.securevotebackend.Repo.ElectionRepo;
import com.securevote.securevotebackend.Repo.UserRepo;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ElectionService {
    private static final Logger log = LoggerFactory.getLogger(ElectionService.class);

    @Autowired
    private ElectionRepo electionRepo;

    @Autowired
    private UserRepo userRepo;

    @Transactional
    public Election saveElection(Election election){
        if(election.getCreatedBy() == null || election.getCreatedBy().getId() == null){
            throw new IllegalArgumentException("Creator is required for election");
        }
        Optional<User> creatorOpt = userRepo.findById(election.getCreatedBy().getId());
        if(creatorOpt.isEmpty()){
            throw new IllegalArgumentException("Creator not found for id: "+election.getCreatedBy().getId());
        }
        election.setCreatedBy(creatorOpt.get());
        election.setCreatedAt(LocalDateTime.now());
        election.setUpdatedAt(LocalDateTime.now());
        if(election.getStartTime().isAfter(election.getEndTime())){
            throw new IllegalArgumentException("Start time must be before end time");
        }
        log.info("Saving election: {}",election.getTitle());
        return electionRepo.save(election);
    }
    @Transactional(readOnly = true)
    public Optional<Election> findById(Long id){
        log.info("fetching election by id: {}",id);
        return electionRepo.findById(id);
    }
    @Transactional(readOnly = true)
    public List<ElectionResponseDto> getAllElection(){
        log.info("Fetching all election");
        List<Election> elections = electionRepo.findAll();
        return elections.stream()
                .map(ElectionResponseDto::new)
                .collect(Collectors.toList());
    }

}
