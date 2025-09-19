import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../api.service';
import { Router } from '@angular/router';
import {VotingStatsComponent} from '../voting-stats/voting-stats.component';
import {Candidate} from '../../../candidate.interface';
import {CandidateCardComponent} from '../candidate-card/candidate-card.component';
import {FaceCaptureComponent} from '../face-capture/face-capture.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, VotingStatsComponent, CandidateCardComponent, FaceCaptureComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  totalVotes: number = 0;
  totalCandidates: number = 0;
  timeRemaining: string = '2d 14h';
  security: string = '100%';
  candidates: Candidate[] = [];
  walletAddress: string | null = localStorage.getItem('walletAddress');
  selectedCandidateId: number | null = null;
  showFaceCapture: boolean = false;
  errorMessage: string = '';

  constructor(private apiService: ApiService, private router: Router) {}

  ngOnInit(): void {
    if (!this.walletAddress) {
      this.router.navigate(['/login']);
      return;
    }

    this.loadCandidates();
  }

  loadCandidates(): void {
    this.apiService.getCandidates().subscribe({
      next: (candidates) => {
        this.candidates = candidates;
        this.updateTotals();
      },
      error: (err) => {
        console.error('Failed to load candidates', err);
        // Fallback mock data
        this.candidates = [
          {
            id: 1,
            name: 'Sarah Mitchell',
            party: 'Progressive Alliance',
            bio: 'Experienced leader focused on sustainable development and community growth.',
            imageUrl: 'https://via.placeholder.com/80x80/ff6b6b/ffffff?text=SM',
            votes: 1247,
            isActive: true,
            createdAt: '2025-09-19T10:00:00'
          },
          {
            id: 2,
            name: 'Michael Chen',
            party: 'Independent',
            bio: 'Business advocate committed to economic development and transparency.',
            imageUrl: 'https://via.placeholder.com/80x80/4ecdc4/ffffff?text=MC',
            votes: 892,
            isActive: true,
            createdAt: '2025-09-19T11:00:00'
          },
          {
            id: 3,
            name: 'Elena Rodriguez',
            party: 'Citizens First',
            bio: 'Education reformer dedicated to improving schools and student outcomes.',
            imageUrl: 'https://via.placeholder.com/80x80/45b7d1/ffffff?text=ER',
            votes: 1156,
            isActive: true,
            createdAt: '2025-09-19T12:00:00'
          }
        ];
        this.updateTotals();
      }
    });
  }
  updateTotals(): void {
    this.totalVotes = this.candidates.reduce((sum,candidate) => sum +candidate.votes, 0);
    this.totalCandidates = this.candidates.length;
  }

  handleVote(candidateId: number): void {
    if(!this.walletAddress){
      this.errorMessage = 'Please log into vote';
      return;
    }
    this.selectedCandidateId = candidateId;
    this.showFaceCapture = true;
  }
  onFaceCapture(base64Image:string):void {
    if(!this.selectedCandidateId || !this.walletAddress){
      this.errorMessage = 'Missing data for verification';
      return;
    }
    const file = this.base64ToFile(base64Image,`vote-face-${Date.now()}.png`);
    this.apiService.verifyForLogin(this.walletAddress!,file).subscribe({
      next: (isVerified) => {
        if(isVerified){
          this.castVote();
        }else{
          this.errorMessage = 'Face verification failed.Please try again or log in.';
          this.showFaceCapture = false;
        }
      },
      error: (err) => {
        this.errorMessage = 'Verification error: '+ (err.error?.message || 'Please try again');
        this.showFaceCapture = false;
      }
    });
  }
  castVote(): void {
    if(!this.selectedCandidateId || !this.walletAddress){
      this.errorMessage = 'Missing data for voting';
      return;
    }
    this.apiService.castVote(this.walletAddress,this.selectedCandidateId,this.image!).subscribe({
      next: (res) =>{
        this.errorMessage = 'Vote cast successfully!.';
        this.loadCandidates();
        this.showFaceCapture = false;
      },
      error: (err) => {
        this.errorMessage = 'Voting failed: '+ (err.error?.message || 'Please try again');
        this.showFaceCapture = false;
      }
    });
  }
  cancelVote(): void {
    this.selectedCandidateId = null;
    this.showFaceCapture = false;
    this.errorMessage = '';
  }
  private base64ToFile(base64: string,fileName:string):File {
    const arr = base64.split(',');
    const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/png';
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while(n--){
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr],fileName,{type:mime});
  }
  get image() : File | null {
    return null;
  }
}
