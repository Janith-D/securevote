import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {Candidate} from '../../../candidate.interface';
import {MatTooltipModule} from '@angular/material/tooltip';


@Component({
  selector: 'app-candidate-card',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './candidate-card.component.html',
  styleUrls: ['./candidate-card.component.css']
})
export class CandidateCardComponent {
  @Input() candidate!: Candidate;
  @Input() totalVotes: number = 0;
  @Output() vote = new EventEmitter<number>();

  onVote() {
    if(this.candidate.isActive){
      this.vote.emit(this.candidate.id);
    }
  }
}
