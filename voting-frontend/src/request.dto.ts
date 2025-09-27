export interface CandidateRequestDto {
  name:string;
  party: string;
  bio: string;
  imageUrl: string;
  election: {id:number};
}
export interface ElectionRequestDto{
  title:string;
  description:string;
  startTime:string;
  endTime:string;
  contractAddress:string;
  createdBy: {id:number};

}
export interface ElectionResponseDto {
  id:number;
  title:string;
  description:string;
  startTime:string;
  endTime:string;
  contractAddress:string;
  createdBy: number | null;
  isActivate: boolean;
  createdAt: string;
}
export interface CandidateResponseDto {
  id:number;
  name: string;
  party: string;
  bio: string;
  imageUrl: string;
  votes: number;
  isActivate: boolean;
  createdAt: string;
}
export interface UserResponseDto {
  id: number;
  username: string;
  email: string;
  walletAddress: string;
  role: string;
  verified: boolean;
  faceRegistered: boolean;
  createdAt: string;
}
