package com.securevote.securevotebackend.Controller;

import com.securevote.securevotebackend.Entity.Election;
import com.securevote.securevotebackend.Service.ElectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {
    @Autowired
    private ElectionService electionService;

    @PostMapping("/createElection")
    public ResponseEntity<Election> createElection(@RequestBody Election election) {
        try {
            Election savedElection = electionService.saveElection(election);
            return ResponseEntity.ok(savedElection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Election> getElection(@PathVariable Long id) {
        return electionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/getAllElection")
    public ResponseEntity<List<Election>> getAllElection(){
        List<Election> elections = electionService.getAllElection();
        return ResponseEntity.ok(elections);
    }
}
