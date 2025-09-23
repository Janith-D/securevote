import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatTableModule} from '@angular/material/table';
import {MatInputModule} from '@angular/material/input';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ApiService} from '../../api.service';
import {Router} from '@angular/router';
import {CandidateResponseDto, ElectionResponseDto, UserResponseDto} from '../../../request.dto';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule,MatCardModule,MatButtonModule,MatTableModule,MatInputModule,ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  users: any[] =[];
  candidates: any[] = [];
  elections: any[] = [];
  candidateForm: FormGroup;
  electionForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';

  //Toggle status for forms
  showCandidateForm: boolean = false;
  showElectionForm: boolean = false;

  //Selected item for details
  selectedUser: UserResponseDto | null = null;
  selectedCandidate: CandidateResponseDto | null = null;
  selectedElection: ElectionResponseDto | null = null;

  //table column definition
  displayedColumnsUser: string[] =['id','username','email','walletAddress','role'];
  displayedColumnsCandidate: string[] =['id','name','bio','votes','party'];
  displayedColumnsElection: string[] =['id','title','startTime','endTime','contractAddress'];


  constructor(private apiService: ApiService,private fb: FormBuilder, private router: Router) {
    this.candidateForm = this.fb.group({
      name: [''],
      bio: [''],
      imageUrl: [''],
      party: [''],
    });
    this.electionForm = this.fb.group({
      title: [''],
      description: [''],
      startTime: [''],
      endTime: [''],
      contractAddress: [''],
    });
  }

  ngOnInit() {
    const walletAddress = localStorage.getItem('walletAddress');
    const role = localStorage.getItem('role');
    if(!walletAddress || role !== 'ADMIN'){
      this.router.navigate(['/login']);
      return;
    }
    this.loadData();
  }
  loadData(): void{
    this.apiService.getAllUsers().subscribe({
      next: (users) => this.users = users,
      error: (err) => this.errorMessage = 'Faled to load users:' + err.message
    });
    this.apiService.getCandidates().subscribe({
      next: (candidates) => this.candidates = candidates,
      error: (err) => this.errorMessage = 'Faled to load candidates:' + err.message
    });
    this.apiService.getElection().subscribe({
      next: (election) => this.elections = election,
      error: (err) => this.errorMessage = 'Faled to load election:' + err.message
    });
  }
  createCandidate(): void {
    if(this.candidateForm.valid){
      const candidate = this.candidateForm.value;
      this.apiService.createCandidate(candidate).subscribe({
        next: () => {
          this.successMessage = 'Candidate created successfully.';
          this.loadData();
          this.candidateForm.reset();
        },
        error: (err) => this.errorMessage = 'Faled to create candidate:' + err.message
      });
    }
  }
  createElection(): void {
    if(this.electionForm.valid) {
      const election = this.electionForm.value;
      this.apiService.createElection(election).subscribe({
        next: () => {
          this.successMessage = 'Election created successfully.';
          this.loadData();
          this.electionForm.reset();
        },
        error: (err) => this.errorMessage = 'Faled to create election:' + err.message
      });
    }
  }
  toggleCandidateForm(): void {
    this.showCandidateForm = !this.showCandidateForm;
    if(this.showCandidateForm) this.showElectionForm = false;
  }
  toggleElectionForm(): void {
    this.showElectionForm = !this.showElectionForm;
    if(this.showElectionForm) this.showCandidateForm = false;
  }

  showCandidateDetails(candidate: any): void{
    this.selectedCandidate = this.selectedCandidate === candidate ? null:candidate;
    this.selectedUser = null;
    this.selectedElection = null;
  }
  showElectionDetails(election: any): void {
    this.selectedElection = this.selectedElection === election ? null : election;
    this.selectedUser = null;
    this.selectedCandidate = null;
  }
  showUserDetails(user: UserResponseDto): void {
    this.selectedUser = this.selectedUser === user ? null : user;
    this.selectedCandidate = null;
    this.selectedElection = null;
  }
}

