import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { Router } from '@angular/router';
import { CandidateResponseDto, ElectionResponseDto, UserResponseDto } from '../../../request.dto';
import { MatSelectModule } from '@angular/material/select';
// @ts-ignore
import { MatDatepickerModule } from '@angular/material/datepicker';
import {MatNativeDateModule} from '@angular/material/core';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatInputModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  users: UserResponseDto[] = [];
  candidates: CandidateResponseDto[] = [];
  elections: ElectionResponseDto[] = [];
  candidateForm: FormGroup;
  electionForm: FormGroup;
  errorMessage: string = '';
  successMessage: string = '';

  // Toggle status for forms
  showCandidateForm: boolean = false;
  showElectionForm: boolean = false;

  // Selected item for details
  selectedUser: UserResponseDto | null = null;
  selectedCandidate: CandidateResponseDto | null = null;
  selectedElection: ElectionResponseDto | null = null;

  // Table column definitions
  displayedColumnsUser: string[] = ['id', 'username', 'email', 'walletAddress', 'role'];
  displayedColumnsCandidate: string[] = ['id', 'name', 'bio', 'votes', 'party'];
  displayedColumnsElection: string[] = ['id', 'title', 'startTime', 'endTime', 'contractAddress'];

  constructor(private apiService: ApiService, private fb: FormBuilder, private router: Router) {
    this.candidateForm = this.fb.group({
      name: ['', Validators.required],
      bio: [''],
      imageUrl: [''],
      party: [''],
      electionId: [null, Validators.required]
    });
    this.electionForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      startTime: ['', Validators.required],
      endTime: ['', Validators.required],
      contractAddress: [''],
      createdBy: [null, Validators.required]
    });
  }

  ngOnInit() {
    const walletAddress = localStorage.getItem('walletAddress');
    const role = localStorage.getItem('role');
    console.log('Wallet Address:', walletAddress, 'Role:', role); // Debug log
    if (!walletAddress || role !== 'ADMIN') {
      this.router.navigate(['/login']);
      return;
    }
    this.loadData();
  }

  loadData(): void {
    this.apiService.getAllUsers().subscribe({
      next: (users) => this.users = users,
      error: (err) => this.errorMessage = 'Failed to load users: ' + err.message
    });
    this.apiService.getCandidates().subscribe({
      next: (candidates) => this.candidates = candidates,
      error: (err) => this.errorMessage = 'Failed to load candidates: ' + err.message
    });
    this.apiService.getElection().subscribe({
      next: (elections) => this.elections = elections,
      error: (err) => this.errorMessage = 'Failed to load elections: ' + err.message
    });
  }

  createCandidate(): void {
    if (this.candidateForm.valid) {
      const candidate = {
        ...this.candidateForm.value,
        election: { id: this.candidateForm.value.electionId }
      };
      this.apiService.createCandidate(candidate).subscribe({
        next: () => {
          this.successMessage = 'Candidate created successfully.';
          this.loadData();
          this.candidateForm.reset({ electionId: null });
          this.showCandidateForm = false;
        },
        error: (err) => this.errorMessage = 'Failed to create candidate: ' + err.message
      });
    }
  }

  createElection(): void {
    if (this.electionForm.valid) {
      const election = {
        ...this.electionForm.value,
        startTime: this.electionForm.value.startTime ? this.electionForm.value.startTime.toISOString() : null,
        endTime: this.electionForm.value.endTime ? this.electionForm.value.endTime.toISOString() : null,
        createdBy: { id: this.electionForm.value.createdBy }
      };
      console.log('Sending Election Payload:', election);
      this.apiService.createElection(election).subscribe({
        next: (response) => {
          console.log('Response from createElection:', response);
          this.successMessage = 'Election created successfully.';
          this.loadData();
          this.electionForm.reset();
          this.showElectionForm = false;
        },
        error: (err) => {
          this.errorMessage = `Failed to create election: ${err.message}`;
          console.error('Error Details:', {
            status: err.status,
            statusText: err.statusText,
            error: err.error,
            url: err.url,
            headers: err.headers,
            body: err.error ? (typeof err.error === 'string' ? err.error : JSON.stringify(err.error)) : 'No body'
          });
        }
      });
    } else {
      console.warn('Form is invalid:', this.electionForm.errors);
    }
  }
  toggleCandidateForm(): void {
    setTimeout(() => this.showCandidateForm = !this.showCandidateForm, 10);
    if (this.showCandidateForm) this.showElectionForm = false;
  }

  toggleElectionForm(): void {
    setTimeout(() => this.showElectionForm = !this.showElectionForm, 10);
    if (this.showElectionForm) this.showCandidateForm = false;
  }

  showCandidateDetails(candidate: CandidateResponseDto): void {
    this.selectedCandidate = this.selectedCandidate === candidate ? null : candidate;
    this.selectedUser = null;
    this.selectedElection = null;
  }

  showElectionDetails(election: ElectionResponseDto): void {
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
