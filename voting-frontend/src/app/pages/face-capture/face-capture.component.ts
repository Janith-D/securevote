import {Component, EventEmitter, OnDestroy, Output} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-face-capture',
  standalone: true,
  imports: [CommonModule, MatCardModule,MatButtonModule],
  templateUrl: './face-capture.component.html',
  styleUrl: './face-capture.component.css'
})
export class FaceCaptureComponent implements OnDestroy{
  @Output() capture = new EventEmitter<string>();
  videoElement !: HTMLVideoElement;
  private stream : MediaStream | null = null;

  async ngAfterViewInit() {
    this.videoElement = document.querySelector('#videoElement') as HTMLVideoElement;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({video:true});
      this.videoElement.srcObject = stream;
    }catch(err) {
      console.error('Error accesing camera',err);
    }
  }
  captureFace(){
    const canvas = document.createElement('canvas');
    canvas.width = this.videoElement.videoWidth;
    canvas.height = this.videoElement.videoHeight;

    const ctx = canvas.getContext('2d');
    ctx?.drawImage(this.videoElement, 0, 0, canvas.width, canvas.height);

    const imageData = canvas.toDataURL('image/png');
    this.capture.emit(imageData); // emit base64 image

    this.stopCamera();
  }
  stopCamera(){
    if(this.stream){
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }
    if(this.videoElement){
      this.videoElement.srcObject = null;
    }
  }
  ngOnDestroy() {
    // Make sure camera is off if user leaves component
    this.stopCamera();
  }
}

