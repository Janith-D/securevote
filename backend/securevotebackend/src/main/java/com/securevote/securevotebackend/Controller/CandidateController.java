package com.securevote.securevotebackend.Controller;

import com.securevote.securevotebackend.Dto.CandidateDto;
import com.securevote.securevotebackend.Entity.Candidate;
import com.securevote.securevotebackend.Service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    @Autowired
    private CandidateService candidateService;

    @PostMapping("/create")
    public ResponseEntity<Candidate> createCandidate(@RequestBody Candidate candidate){
        try {
            Candidate savedCandidate = candidateService.saveCandidate(candidate);
            return ResponseEntity.ok(savedCandidate);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(null);
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Candidate> getCandidate(@PathVariable Long id){
        return candidateService.findById(id)
                .map(ResponseEntity :: ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/getAllCandidates")
    public ResponseEntity<Map<String,Object>> getAllCandidates(){
        List<CandidateDto> candidates = candidateService.getAllCandidates();
        Map<String,Object> response = new HashMap<>();
        if(!candidates.isEmpty()){
            response.put("status","success");
            response.put("candidates",candidates);
        }else{
            response.put("status","error");
            response.put("message","No candidates found");
        }
        return ResponseEntity.ok(response);
    }
}
