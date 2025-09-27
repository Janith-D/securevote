import { Injectable } from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse} from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';
import {CandidateRequestDto, ElectionRequestDto, ElectionResponseDto, UserResponseDto} from '../request.dto';


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
        if (response && response.status === 'success' && response.role) {
          return {role: response.role.toUpperCase(),walletAddress: response.walletAddress || walletAddress};
        }
        return { role: 'VOTER', walletAddress }; // Fallback
      }),
      catchError(this.handleError)
    );
  }
// api.service.ts
  castVote(walletAddress: string, candidateId: number, file: File, electionId: number): Observable<any> {
    const formData = new FormData();
    formData.append('candidateId', candidateId.toString());
    formData.append('electionId',electionId.toString());
    formData.append('image', file);
    return this.http.post<any>(`${this.baseUrl}/voting/vote`, formData, {
      headers: { 'X-Wallet-Address': walletAddress }
    }).pipe(
      catchError(this.handleError)
    );
  }
  //add getVotesByWalletAddress if not present
  getVotesByWalletAddress(walletAddress:string): Observable<any[]>{
    return this.http.get<any[]>(`${this.baseUrl}/voting/${walletAddress}`,{
      headers: { 'X-Wallet-Address': walletAddress }
    }).pipe(
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
  createCandidate(candidate: CandidateRequestDto): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/candidates/create`, candidate,{
      headers:{'X-Wallet-Address': localStorage.getItem('walletAddress') || ''}
    }).pipe(
      catchError(this.handleError)
    );
  }
  createElection(election: ElectionRequestDto): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/elections/createElection`,election,{
      headers:{'X-Wallet-Address': localStorage.getItem('walletAddress') || ''},
      observe: 'response'
    }).pipe(
      map((response: HttpResponse<any>)=>{
        if(response.status === 201 || response.status === 200) {
          return response.body;
        }
        throw new Error(`Unexpected error : ${response.status}`);
      }),
      catchError(this.handleError)
    );
  }
  getElection(): Observable<any[]> {
    return this.http.get<any>(`${this.baseUrl}/elections/getAllElection`,{
      headers:{'X-Wallet-Address': localStorage.getItem('walletAddress') || ''}
    }).pipe(
      map(response => {
        return Array.isArray(response)? response: [];
      }),
      catchError(this.handleError)
    );
  }
  getUser(walletAddress:string): Observable<UserResponseDto> {
    return this.http.get<any>(`${this.baseUrl}/voting/user/${walletAddress}}`,{
      headers:{'X-Wallet-Address': localStorage.getItem('walletAddress') || ''}
    }).pipe(
      catchError(this.handleError)
    );
  }
  getAllUsers(): Observable<UserResponseDto[]>{
    return this.http.get<any>(`${this.baseUrl}/voting/getAllUsers`,{
      headers:{'X-Wallet-Address': localStorage.getItem('walletAddress') || ''}
    }).pipe(
      catchError(this.handleError)
    );
  }
}
