export interface Candidate {
  id: number;
  name:string;
  bio: string;
  imageUrl?: string;
  votes:number;
  party: string;
  isActive: boolean;
  createdAt?: string;
}
