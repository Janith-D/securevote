import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VotingStatsComponent } from './voting-stats.component';

describe('VotingStatsComponent', () => {
  let component: VotingStatsComponent;
  let fixture: ComponentFixture<VotingStatsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VotingStatsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VotingStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
