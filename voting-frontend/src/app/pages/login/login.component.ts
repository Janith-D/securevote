import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormField, MatInput, MatInputModule } from '@angular/material/input';
import { MatButton, MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FaceCaptureComponent } from '../face-capture/face-capture.component';
import { NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../api.service';
import { map, catchError } from 'rxjs/operators'; // Add these imports

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [MatFormFieldModule, FormsModule, MatInputModule, FaceCaptureComponent, NgIf, MatButtonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  walletAddress = '';
  image: File | null = null;
  imageCaptured = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;
  isLoggingIn = false;

  constructor(private apiService: ApiService, private router: Router) {}

  onFaceCapture(base64Image: string) {
    const file = this.base64ToFile(base64Image, `login-face-${Date.now()}.png`);
    this.image = file;
    this.imageCaptured = true;
    this.errorMessage = null;
    this.successMessage = 'Face captured successfully. Ready to verify.';
  }

  private base64ToFile(base64: string, fileName: string): File {
    const arr = base64.split(',');
    const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/png';
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], fileName, { type: mime });
  }

  onLogin() {
    if (!this.image || !this.walletAddress || !this.walletAddress.trim()) {
      this.errorMessage = 'Please enter your wallet address and capture your face';
      return;
    }
    this.isLoggingIn = true;
    this.errorMessage = null;
    this.successMessage = null;

    const formData = new FormData();
    formData.append('walletAddress', this.walletAddress.trim());
    formData.append('image', this.image);

    this.apiService.verifyForLogin(this.walletAddress.trim(), this.image).subscribe({
      next: (res: any) => {
        this.successMessage = 'Login successful!';
        this.isLoggingIn = false;
        // Store user data in localStorage
        localStorage.setItem('walletAddress', this.walletAddress.trim());
        localStorage.setItem('role', res.role || 'VOTER'); // Access res.role directly
        //role base navigation
        const role = res.role.toUpperCase();
        if(role === 'ADMIN'){
          setTimeout(() => this.router.navigate(['/admin-dashboard']),1000);
        }else {
          setTimeout(() => this.router.navigate(['/dashboard']), 1000); // Redirect to dashboard
        }
      },
      error: (err) => {
        this.isLoggingIn = false;
        if (err.status === 400 || err.error?.message?.includes('Face verification failed')) {
          this.errorMessage = 'Face verification failed. Please try again or adjust your position.';
        } else if (err.status === 404 || err.error?.message?.includes('User not found')) {
          this.errorMessage = 'Wallet address not found. Please register first.';
        } else if (err.status === 500) {
          this.errorMessage = 'Login failed due to a server error. Please try again later.';
        } else if (err.status === 403) {
          this.errorMessage = 'Access denied. Please contact support.';
        } else {
          this.errorMessage = 'Login failed. Please try again or check your details.';
        }
        this.successMessage = null;
        console.error('Login error', { status: err.status, message: err.message, error: err.error });
      }
    });
  }
}
