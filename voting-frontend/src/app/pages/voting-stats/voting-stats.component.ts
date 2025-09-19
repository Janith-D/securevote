import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';

@Component({
  selector: 'app-voting-stats',
  standalone: true,
  imports: [CommonModule,MatCardModule,MatIconModule,MatListModule],
  templateUrl: './voting-stats.component.html',
  styleUrls: ['./voting-stats.component.css']
})
export class VotingStatsComponent {
  @Input() totalVotes: number=0;
  @Input() totalCandidates: number=0;
  @Input() timeRemaining:string = '';
  @Input() security: string = '';
}
