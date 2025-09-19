import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../api.service';
import { FormsModule } from '@angular/forms';
import { MatFormField, MatInput, MatInputModule } from '@angular/material/input';
import { MatButton, MatButtonModule } from '@angular/material/button';
import { FaceCaptureComponent } from '../face-capture/face-capture.component';
import { CommonModule, NgIf } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    FaceCaptureComponent,
    NgIf,
    MatButtonModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  data = { username: '', email: '', walletAddress: '', firstName: '', lastName: '' }; // Removed password
  image: File | null = null;
  imageCaptured = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(private apiService: ApiService, private router: Router) { }

  onFaceCapture(image: string) {
    const file = this.base64ToFile(image, 'captured-face.png');
    this.image = file;
    this.imageCaptured = true;
    this.errorMessage = null;
    this.successMessage = 'Face captured successfully.';
  }

  // Helper method
  base64ToFile(base64: string, filename: string): File {
    const arr = base64.split(',');
    const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/png';
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], filename, { type: mime });
  }

  onRegister() {
    if (!this.image || !this.data.walletAddress) {
      this.errorMessage = 'Please capture your face and provide a wallet address.';
      return;
    }

    const formData = new FormData();
    formData.append('username', this.data.username || '');
    formData.append('email', this.data.email || '');
    formData.append('walletAddress', this.data.walletAddress || '');
    formData.append('firstName', this.data.firstName || '');
    formData.append('lastName', this.data.lastName || '');
    formData.append('image', this.image);

    this.apiService.register(this.data, this.image).subscribe({ // Update this line to pass formData instead
      next: (res: any) => {
        this.successMessage = res.message || 'Registration successful.';
        this.errorMessage = null;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        if (err.status === 409 || err.error?.message?.includes('already registered')) {
          this.errorMessage = 'This wallet address already exists.';
        } else if (err.status === 400) {
          this.errorMessage = 'Registration failed: Invalid data. Please check your wallet address or image.';
        } else if (err.status === 500) {
          this.errorMessage = 'Registration failed due to a server error. Please try again later.';
        } else if (err.status === 403) {
          this.errorMessage = 'Access denied. The server rejected the request. Please contact support if this persists.';
        } else {
          this.errorMessage = 'Registration failed. Please try again or check your details.';
        }
        this.successMessage = null;
        console.error('Registration error', { status: err.status, message: err.message, error: err.error });
      }
    });
  }
}
