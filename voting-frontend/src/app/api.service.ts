import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  // Handle HTTP errors
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'An error has occurred';
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message || error.error.message}`;
    }
    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }

  // Register a new voter with face image
  register(data: { username: string, email: string, walletAddress: string, firstName?: string, lastName?: string }, image: File): Observable<any> {
    const formData = new FormData();
    formData.append('username', data.username || '');
    formData.append('email', data.email || '');
    formData.append('walletAddress', data.walletAddress || '');
    formData.append('firstName', data.firstName || '');
    formData.append('lastName', data.lastName || '');
    formData.append('image', image);

    return this.http.post(`${this.baseUrl}/voting/register`, formData).pipe(
      catchError(this.handleError)
    );
  }
  // verify face for login
  // api.service.ts
  verifyForLogin(walletAddress: string, image: File): Observable<any> {
    const formData = new FormData();
    formData.append('walletAddress', walletAddress);
    formData.append('image', image);
    return this.http.post<any>(`${this.baseUrl}/voting/verify`, formData).pipe(
      map(response => {
        if (response && response.role) {
          return response; // { role: "voter", walletAddress: "..." }
        }
        return { role: 'VOTER', walletAddress }; // Fallback
      }),
      catchError(this.handleError)
    );
  }
  castVote(walletAddress: string, candidateId: number,image: File): Observable<any> {
    const formData = new FormData();
    formData.append('walletAddress',walletAddress);
    formData.append('candidateId',candidateId.toString());
    formData.append('image', image);
    return this.http.post<any>(`${this.baseUrl}/voting/castVote`, formData).pipe(
      map(response => {
        if(response && response.status === 'success'){
          return response;
        }
        throw new Error('Voting failed');
      }),
      catchError(this.handleError)
    );
  }
  getCandidates(): Observable<any[]> {
    return this.http.get<any>(`${this.baseUrl}/candidates/getAllCandidates`).pipe(
      map(response => {
        if(response && response.status === 'success'){
          return response.candidates.map((candidate:any) => ({
            id: candidate.id,
            name: candidate.name,
            bio: candidate.bio || candidate.description,
            imageUrl: candidate.imageUrl,
            votes: candidate.voteCount || 0,
            party:'independent',
            isActive: candidate.isActive != false,
            createdAt: candidate.createdAt
          }));
        }
        return [];
      }),
      catchError(this.handleError)
    );
  }
}
